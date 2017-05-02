package com.atg.openssp.core.exchange.channel.rtb;

import java.util.concurrent.Callable;

import org.apache.http.Header;
import org.apache.http.message.BasicHeader;
import org.openrtb.validator.OpenRtbInputType;
import org.openrtb.validator.OpenRtbValidator;
import org.openrtb.validator.OpenRtbValidatorFactory;

import com.atg.openssp.common.core.broker.AbstractBroker;
import com.atg.openssp.common.demand.ResponseContainer;
import com.atg.openssp.common.demand.Supplier;
import com.atg.openssp.common.logadapter.RtbRequestLogProcessor;
import com.atg.openssp.common.logadapter.RtbResponseLogProcessor;
import com.atg.service.LogFacade;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;

import openrtb.bidrequest.model.BidRequest;
import openrtb.bidresponse.model.BidResponse;

/**
 * This class acts as Broker to a connector used in demand (OpenRTB) context. It represents one Demand side (DSP).
 * 
 * @author André Schmer
 *
 */
public final class DemandBroker extends AbstractBroker implements Callable<ResponseContainer> {

	private final Supplier supplier;

	private final OpenRtbConnector connector;

	private final Header[] headers;

	private final Gson gson;

	private static OpenRtbValidator responseValidator;

	public DemandBroker(final Supplier supplier, final OpenRtbConnector connector) {
		this.supplier = supplier;
		this.connector = connector;

		headers = new Header[2];
		headers[0] = new BasicHeader("x-openrtb-version", supplier.getOpenRtbVersion());
		headers[1] = new BasicHeader("ContentType", supplier.getContentType());
		headers[2] = new BasicHeader("Accept-Encoding", supplier.getAcceptEncoding());
		headers[3] = new BasicHeader("Content-Encoding", supplier.getContentEncoding());

		gson = new GsonBuilder().setVersion(Double.valueOf(supplier.getOpenRtbVersion())).create();
		responseValidator = OpenRtbValidatorFactory.getValidatorWithFactual(OpenRtbInputType.BID_RESPONSE, supplier.getOpenRtbVersion());
	}

	@Override
	public ResponseContainer call() throws Exception {
		try {
			final String jsonBidrequest = gson.toJson(sessionAgent.getBidExchange().getBidRequest(supplier).build(), BidRequest.class);

			// To decide: BidRequest validation necessary?
			// final OpenRtbValidator requestValidator = OpenRtbValidatorFactory.getValidator(OpenRtbInputType.BID_REQUEST, supplier.getOpenRtbVersion());
			// if (!requestValidator.isValid(jsonBidrequest)) {
			// System.out.println("request is not valid");
			// }

			LogFacade.logDebug(jsonBidrequest);
			RtbRequestLogProcessor.instance.setLogData(jsonBidrequest, "bidrequest", String.valueOf(supplier.getSupplierId()));

			final String result = connector.connect(jsonBidrequest, headers);
			LogFacade.logDebug("result: " + result);
			if (result != null) {
				final BidResponse.Builder bidResponse = ResponseParser.parse(result, supplier);
				sessionAgent.getBidExchange().setBidResponse(supplier, bidResponse);
				return new ResponseContainer(supplier, bidResponse);
			}
		} catch (final Exception ignore) {
			LogFacade.logDebug(ignore.getMessage());
		}
		return null;
	}

	public Supplier getSupplier() {
		return supplier;
	}

	private static class ResponseParser {
		private static Gson gson = new Gson();

		private static BidResponse.Builder parse(final String json, final Supplier supplier) {
			RtbResponseLogProcessor.instance.setLogData(json, "bidresponse", String.valueOf(supplier.getSupplierId()));
			try {
				if (responseValidator.isValid(json)) {
					final BidResponse response = gson.fromJson(json, BidResponse.class);
					return response.getBuilder();
				}

			} catch (final JsonIOException | JsonSyntaxException e) {
				LogFacade.logException(ResponseParser.class, e.getMessage());
			}
			return null;
		}
	}
}
