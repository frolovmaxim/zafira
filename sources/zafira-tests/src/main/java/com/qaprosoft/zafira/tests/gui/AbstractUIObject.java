package com.qaprosoft.zafira.tests.gui;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.pagefactory.ElementLocatorFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Wait;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.qaprosoft.zafira.tests.util.Config;
import com.qaprosoft.zafira.tests.util.webdriver.SearchElementLocatorFactory;
import com.qaprosoft.zafira.tests.util.webdriver.UIElementDecorator;

public abstract class AbstractUIObject
{

	protected static final Logger LOGGER = Logger.getLogger(AbstractPage.class);
	protected static final Long IMPLICITLY_TIMEOUT = 25L;
	protected static final int ADMIN_ID = Integer.valueOf(Config.get("admin1.id"));
	protected static final int PERFORMANCE_DASHBOARD_ID = Integer.valueOf(Config.get("dashboard.performance.id"));

	protected WebDriver driver;
	protected final SearchContext context;
	protected WebElement rootElement;
	protected String fieldName;

	@FindBy(css = "md-backdrop")
	private WebElement backdrop;

	@FindBy(css = "md-backdrop:not(.ng-animate)")
	private WebElement backdropNotAnimate;

	@FindBy(xpath = "//md-tooltip")
	private WebElement tooltip;

	@FindBy(xpath = "//div[contains(@class, 'ajs-success')]")
	private WebElement successAlert;

	@FindBy(xpath = "//div[contains(@class, 'ajs-error')]")
	private WebElement errorAlert;

	@FindBy(xpath = "//div[contains(@class, 'ajs-warning')]")
	private WebElement warningAlert;

	protected AbstractUIObject(WebDriver driver)
	{
		this(driver, driver);
	}

	protected AbstractUIObject(WebDriver driver, SearchContext context)
	{
		this.driver = driver;
		this.context = context;
		ElementLocatorFactory elementLocatorFactory = new SearchElementLocatorFactory(context);
		PageFactory.initElements(new UIElementDecorator(driver, elementLocatorFactory), this);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public boolean isElementPresent(By by, long seconds)
	{
		return innerTimeoutOperation(() -> {
			Wait webDriverWait = new WebDriverWait(driver, seconds, 100L);
			webDriverWait.until(dr -> driver.findElement(by).isDisplayed());
			return webDriverWait;
		});
	}

	public boolean isElementNotPresent(By by, long seconds)
	{
		boolean result = true;
		final double pause = 0.2;
		double attempts = seconds / pause;
		while(result)
		{
			result = isElementPresent(by, seconds);
			if(attempts == 0)
				break;
			pause(pause);
			attempts --;
		}
		return result;
	}

	@SuppressWarnings({ "unchecked", "unused", "rawtypes" })
	public boolean isElementPresent(WebElement webElement, By by, long seconds)
	{
		String element = by == null ? "" : by.toString();
		return innerTimeoutOperation(() -> {
			Wait webDriverWait = new WebDriverWait(driver, seconds, 100L);
			webDriverWait.until(dr -> webElement.findElement(by).isDisplayed());
			return webDriverWait;
		});
	}

	public boolean isBackdropPresent(long seconds)
	{
		return isElementPresent(By.xpath("//md-backdrop"), seconds);
	}

	public boolean isBackdropNotPresent(long seconds)
	{
		return isElementNotPresent(By.xpath("//md-backdrop"), seconds);
	}

	public boolean isBackdropAnimatePresent(long seconds)
	{
		return isElementPresent(By.xpath("//md-backdrop:not(.ng-animate)"), seconds);
	}

	public boolean isBackdropAnimateNotPresent(long seconds)
	{
		return isElementNotPresent(By.xpath("//md-backdrop:not(.ng-animate)"), seconds);
	}

	public boolean isElementPresent(WebElement webElement, long seconds)
	{
		return waitUntilElementIsPresent(webElement, seconds);
	}

	public boolean isElementClickable(WebElement webElement, long seconds)
	{
		return waitUntilElementToBeClickable(webElement, seconds);
	}

	public boolean waitUntilElementIsPresent(By by, long seconds)
	{
		return innerTimeoutOperation(() -> {
			WebDriverWait webDriverWait = new WebDriverWait(driver, seconds, 0L);
			webDriverWait.until(ExpectedConditions.visibilityOfElementLocated(by));
			return webDriverWait;
		});
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public boolean waitUntilElementIsPresent(WebElement webElement, By by, long seconds)
	{
		return innerTimeoutOperation(() -> {
			Wait webDriverWait = new WebDriverWait(driver, seconds, 0L);
			webDriverWait.until(dr -> webElement.findElement(by).isDisplayed());
			return webDriverWait;
		});
	}

	public boolean waitUntilElementIsPresent(WebElement webElement, long seconds)
	{
		return innerTimeoutOperation(() -> {
			WebDriverWait webDriverWait = new WebDriverWait(driver, seconds, 0L);
			webDriverWait.until(ExpectedConditions.visibilityOf(webElement));
			return webDriverWait;
		});
	}

	public boolean waitUntilElementIsNotPresent(WebElement webElement, long seconds)
	{
		return innerTimeoutOperation(() -> {
			WebDriverWait webDriverWait = new WebDriverWait(driver, seconds, 0L);
			webDriverWait.until(ExpectedConditions.invisibilityOfAllElements(Collections.singletonList(webElement)));
			return webDriverWait;
		});
	}

	public boolean waitUntilElementWithTextIsPresent(WebElement webElement, String text, long seconds)
	{
		return innerTimeoutOperation(() -> {
			WebDriverWait webDriverWait = new WebDriverWait(driver, seconds, 0L);
			webDriverWait.until(ExpectedConditions.textToBePresentInElement(webElement, text));
			return webDriverWait;
		});
	}

	public boolean waitUntilAlertWithTextIsPresent(long seconds)
	{
		return innerTimeoutOperation(() -> {
			WebDriverWait webDriverWait = new WebDriverWait(driver, seconds, 0L);
			webDriverWait.until(ExpectedConditions.textMatches(By.xpath("//div[contains(@class, 'ajs') and not(contains(@class, 'ajs-right') )]"), Pattern.compile("^(?=\\s*\\S).*$")));
			pause(0.5);
			return webDriverWait;
		});
	}

	public boolean waitUntilElementToBeClickable(WebElement webElement, long seconds)
	{
		return innerTimeoutOperation(() -> {
			WebDriverWait webDriverWait = new WebDriverWait(driver, seconds, 0L);
			webDriverWait.until(ExpectedConditions.elementToBeClickable(webElement));
			return webDriverWait;
		});
	}

	public boolean waitUntilElementToBeClickableByBackdropMask(WebElement webElement, long seconds)
	{
		return isBackdropNotPresent(seconds) && ! isBackdropAnimatePresent(2) & waitUntilElementToBeClickable(webElement, seconds);
	}

	public boolean waitUntilElementToBeClickableWithBackdropMask(WebElement webElement, long seconds)
	{
		return isBackdropPresent(seconds) && isBackdropAnimateNotPresent(seconds) && waitUntilElementToBeClickable(webElement, seconds);
	}

	@SuppressWarnings("rawtypes")
	private boolean innerTimeoutOperation(Supplier<Wait> operationSupplier)
	{
		boolean result;
		try
		{
			driver.manage().timeouts().implicitlyWait(0L, TimeUnit.SECONDS);
			operationSupplier.get();
			result = true;
		} catch (Exception e)
		{
			result = false;
		} finally
		{
			driver.manage().timeouts().implicitlyWait(IMPLICITLY_TIMEOUT, TimeUnit.SECONDS);
		}
		return result;
	}

	public void hoverOnElement(WebElement webElement)
	{
		Actions actions = new Actions(driver);
		actions.moveToElement(webElement).build().perform();
	}

	public String getTooltipText()
	{
		waitUntilElementIsPresent(tooltip, 1);
		return tooltip.getText();
	}

	public String hoverAndGetTooltipText(WebElement webElement)
	{
		hoverOnElement(webElement);
		return getTooltipText();
	}

	public void clickOutside()
	{
		clickByCoordinates("100", "100");
		isBackdropNotPresent(1);
	}

	public void clickByCoordinates(String x, String y)
	{
		JavascriptExecutor executor = (JavascriptExecutor)driver;
		executor.executeScript(String.format("$(document.elementFromPoint(%s, %s)).click();", x, y));
	}

	public void clickByCoordinates(WebElement element, int x, int y) {
		Actions builder = new Actions(driver);
		builder.moveToElement(element, x, y).click().build().perform();
	}

	public void pause(double timeout)
	{
		try
		{
			Thread.sleep(new Double(timeout * 1000).intValue());
		} catch (InterruptedException e)
		{
			LOGGER.error(e.getMessage());
		}
	}

	public String getWebElementValue(WebElement webElement)
	{
		return webElement.getAttribute("value");
	}

	public boolean hasDisabledAttribute(WebElement webElement)
	{
		return ! StringUtils.isBlank(webElement.getAttribute("disabled")) && ! webElement.getAttribute("disabled").equals("disabled");
	}

	public boolean hasSelectedAttribute(WebElement webElement)
	{
		return ! StringUtils.isBlank(webElement.getAttribute("selected"));
	}

	public WebElement getBackdrop()
	{
		return backdrop;
	}

	public WebElement getBackdropNotAnimate()
	{
		return backdropNotAnimate;
	}

	public WebElement getTooltip()
	{
		return tooltip;
	}

	public WebElement getSuccessAlert()
	{
		return successAlert;
	}

	public WebElement getErrorAlert()
	{
		return errorAlert;
	}

	public WebElement getWarningAlert()
	{
		return warningAlert;
	}

	public WebElement getRootElement()
	{
		return rootElement;
	}

	public void setRootElement(WebElement rootElement)
	{
		this.rootElement = rootElement;
	}

	public String getFieldName()
	{
		return fieldName;
	}

	public void setFieldName(String fieldName)
	{
		this.fieldName = fieldName;
	}

	public boolean isElementPresent(long seconds)
	{
		return isElementPresent(getRootElement(), seconds);
	}

	public void clearAllInputs()
	{
		getRootElement().findElements(By.xpath(".//input[not(@type = 'checkbox') and not(@disabled)] | .//textarea"))
				.forEach(this::clearInput);
	}

	public void clearInput(WebElement input)
	{
		while(! input.getAttribute("value").isEmpty())
		{
			input.click();
			input.sendKeys(Keys.BACK_SPACE);
		}
	}

	public boolean isChecked(WebElement webElement)
	{
		return Arrays.asList(webElement.getAttribute("class").split(" ")).contains("md-checked");
	}

	public void check(WebElement webElement)
	{
		if(! isChecked(webElement))
			webElement.click();
	}

	public void uncheck(WebElement webElement)
	{
		if(isChecked(webElement))
			webElement.click();
	}

	public void switchToWindow()
	{
		Set<String> windowHandles = driver.getWindowHandles();
		windowHandles.remove(driver.getWindowHandle());
		if(windowHandles.size() != 0)
			driver.switchTo().window((String) windowHandles.toArray()[0]);
	}

	public void select(WebElement webElement, String value)
	{
		String id = webElement.getAttribute("aria-owns");
		webElement.click();
		WebElement option = driver.findElement(By.xpath("//div[@id = '" + id + "' and preceding-sibling::header]//md-option[.//*[contains(text(), '" + value + "') "
				+ "or contains(text(), '" + value + "')]]"));
		waitUntilElementToBeClickableWithBackdropMask(option, 1);
		option.click();
	}

	public String getSelectedValue(WebElement webElement)
	{
		return webElement.findElement(By.xpath(".//md-select-value//div")).getText();
	}

	public String getCurrentNodeText(WebElement webElement, boolean replaceChildText)
	{
		String text = webElement.getText();
		driver.manage().timeouts().implicitlyWait(0, TimeUnit.SECONDS);
		if(replaceChildText)
		{
			try {
				List<WebElement> childs = webElement.findElements(By.xpath("./*"));
				for (WebElement child : childs) {
					text = text.replaceFirst(child.getText(), "");
				}
			} finally {
				driver.manage().timeouts().implicitlyWait(IMPLICITLY_TIMEOUT, TimeUnit.SECONDS);
			}
		}
		return text.trim();
	}

	public Alert getAlert()
	{
		return driver.switchTo().alert();
	}
}
