package edu.uoc.elc.lti.platform.deeplinking;

import edu.uoc.elc.lti.exception.InvalidLTICallException;
import edu.uoc.elc.lti.platform.PlatformClient;
import edu.uoc.elc.lti.tool.deeplinking.Settings;
import edu.uoc.lti.deeplink.DeepLinkingResponse;
import edu.uoc.lti.deeplink.DeepLinkingTokenBuilder;
import edu.uoc.lti.deeplink.content.Item;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Xavi Aracil <xaracil@uoc.edu>
 */
@RequiredArgsConstructor
@Getter
@Setter
public class DeepLinkingClient {

	private final DeepLinkingTokenBuilder deepLinkingTokenBuilder;

	private final String platformName;
	private final String toolName;
	private final String azp;
	private final String kid;

	private final String deploymentId;
	private final Settings settings;

	private List<Item> itemList = new ArrayList<>();

	public boolean canAddItem() {
		return settings.isAccept_multiple() || itemList.size() == 0;
	}

	public void addItem(Item item) {
		// check for multiple content item
		if (!canAddItem()) {
			throw new InvalidLTICallException("Platform doesn't allow multiple content items");
		}

		ItemValidator itemValidator = ItemValidatorFactory.itemValidatorFor(item, settings);
		if (!itemValidator.isValid(item)) {
			throw new InvalidLTICallException(itemValidator.getMessage());
		}

		itemList.add(item);
	}

	/**
	 * Performs the DeepLinking response back to the platform
	 */
	public void perform() throws IOException {

		// generate the JWT
		DeepLinkingResponse deepLinkingResponse = new DeepLinkingResponse(platformName,
						toolName, azp, kid, deploymentId, settings.getData(), itemList);
		String token = deepLinkingTokenBuilder.build(deepLinkingResponse);


		URL url = new URL(settings.getDeep_link_return_url());
		// post the JWT back to the platform
		postToService(url, "JWT=" + token);
	}

	private String postToService(URL url, String body) throws IOException {
		PlatformClient platformClient = new PlatformClient();
		return platformClient.post(url, body, null, String.class);
	}
}
