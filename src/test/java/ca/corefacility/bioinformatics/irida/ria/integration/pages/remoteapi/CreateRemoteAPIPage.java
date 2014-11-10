package ca.corefacility.bioinformatics.irida.ria.integration.pages.remoteapi;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.corefacility.bioinformatics.irida.ria.integration.pages.AbstractPage;

public class CreateRemoteAPIPage extends AbstractPage {
	private static final Logger logger = LoggerFactory.getLogger(CreateRemoteAPIPage.class);

	private final String CREATE_PAGE = "remote_api/create";
	public static String SUCCESS_PAGE = "remote_api/\\d+";

	public CreateRemoteAPIPage(WebDriver driver) {
		super(driver);
		get(driver, CREATE_PAGE);
	}

	public void createRemoteAPIWithDetails(String name, String serviceURI, String clientID, String clientSecret) {
		logger.trace("Creating client with name " + name);

		driver.findElement(By.id("name")).sendKeys(name);
		driver.findElement(By.id("clientSecret")).sendKeys(clientSecret);
		driver.findElement(By.id("clientId")).sendKeys(clientID);
		driver.findElement(By.id("serviceURI")).sendKeys(serviceURI);

		WebElement submit = driver.findElement(By.id("create-remoteapi-submit"));
		submit.click();
	}

	public boolean checkSuccess() {
		if (driver.getCurrentUrl().matches(BASE_URL + SUCCESS_PAGE)) {
			return true;
		} else {
			return false;
		}
	}
}
