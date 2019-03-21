package ca.corefacility.bioinformatics.irida.ria.integration.pages.groups;

import java.util.List;
import java.util.stream.Collectors;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.Wait;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.NoSuchElementException;

import ca.corefacility.bioinformatics.irida.ria.integration.pages.AbstractPage;
import ca.corefacility.bioinformatics.irida.ria.integration.utilities.Ajax;

/**
 * Page to represent the group page.
 */
public class GroupsPage extends AbstractPage {
	private static final String CREATE_GROUP_RELATIVE_URL = "/groups/create";
	private static final String GROUP_RELATIVE_URL = "/groups/{groupId}";
	private static final String GROUPS_RELATIVE_URL = "/groups/";

	@FindBy(id = "name")
	private WebElement groupName;

	@FindBy(id = "description")
	private WebElement groupDescription;

	@FindBy(id = "createGroupButton")
	private WebElement createGroupButton;

	@FindBy(id = "add-members-button")
	private WebElement addGroupMemberButton;

	@FindBy(id = "add-user-username")
	private WebElement addGroupMemberUser;

	@FindBy(id = "add-user-role")
	private WebElement addGroupMemberRole;

	@FindBy(id = "submitAddMember")
	private WebElement submitAddMemberButton;

	@FindBy(className = "select2-results__option--highlighted")
	private WebElement select2ResultsOptionHighlighted;

	@FindBy(className = "select2-selection")
	private WebElement userElement;

	@FindBy(className = "select2-search__field")
	private WebElement userNameField;

	public GroupsPage(WebDriver driver) {
		super(driver);
	}

	public static GroupsPage goToCreateGroupPage(WebDriver driver) {
		get(driver, CREATE_GROUP_RELATIVE_URL);
		return PageFactory.initElements(driver, GroupsPage.class);
	}

	public void goToGroupsPage() {
		get(driver, GROUPS_RELATIVE_URL);
	}

	public static GroupsPage goToGroupPage(WebDriver driver, String groupId) {
		get(driver, GROUP_RELATIVE_URL.replace("{groupId}", String.valueOf(groupId)));
		return PageFactory.initElements(driver, GroupsPage.class);
	}

	public List<String> getGroupNames() {
		List<WebElement> els = driver.findElements(By.cssSelector("td:first-child a"));
		return els.stream()
				.map(WebElement::getText)
				.collect(Collectors.toList());
	}

	public void createGroup(String name, String description) {
		waitForElementsVisible(By.id("name"));
		groupName.sendKeys(name);
		waitForElementsVisible(By.id("description"));
		groupDescription.sendKeys(description);
		waitForElementVisible(By.id("createGroupButton"));
		createGroupButton.click();
	}

	public void addGroupMember(String searchTerm, String role) {
		waitForElementVisible(By.id("add-members-button"));
		addGroupMemberButton.click();
		waitForElementVisible(By.className("select2-selection"));
		userElement.click();
		waitForElementVisible(By.className("select2-search__field"));
		userNameField.sendKeys(searchTerm);
		waitForElementVisible(By.className("select2-results__option--highlighted"));
		select2ResultsOptionHighlighted.click();
		Select roleSelect = new Select(addGroupMemberRole);
		roleSelect.selectByValue(role);
		submitAddMemberButton.click();
		waitForAjax();
	}

	public boolean notySuccessDisplayed() {
		boolean present;
		try {
			(new WebDriverWait(driver, 10)).until(
					ExpectedConditions.presenceOfElementLocated(By.className("noty_type__success")));
			present = true;
		} catch (NoSuchElementException e) {
			present = false;
		}

		return present;
	}

	private void waitForAjax() {
		Wait<WebDriver> wait = new WebDriverWait(driver, 60);
		wait.until(Ajax.waitForAjax(60000));
	}
}
