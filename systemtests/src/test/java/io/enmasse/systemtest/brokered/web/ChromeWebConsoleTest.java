package io.enmasse.systemtest.brokered.web;

import io.enmasse.systemtest.Destination;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

public class ChromeWebConsoleTest extends BrokeredWebConsoleTest {

    //@Test
    public void testCreateDeleteQueue() throws Exception {
        doTestCreateDeleteAddress(Destination.queue("test-queue"));
    }

    //@Test
    public void testCreateDeleteTopic() throws Exception {
        doTestCreateDeleteAddress(Destination.topic("test-topic"));
    }

    //@Test
    public void testFilterAddressesByType() throws Exception {
        doTestFilterAddressesByType();
    }

    //@Test
    public void testFilterAddressesByName() throws Exception {
        doTestFilterAddressesByName();
    }

    //@Test
    public void testSortAddressesByName() throws Exception {
        doTestSortAddressesByName();
    }

    //@Test
    public void testSortConnectionsBySenders() throws Exception {
        doTestSortConnectionsBySenders();
    }

    //@Test
    public void testSortConnectionsByReceivers() throws Exception {
        doTestSortConnectionsByReceivers();
    }

    //@Test
    public void testFilterConnectionsByEncrypted() throws Exception {
        doTestFilterConnectionsByEncrypted();
    }

    //@Test
    public void testSortConnectionsByHostname() throws Exception {
        doTestSortConnectionsByHostname();
    }

    @Override
    public WebDriver buildDriver() {
        ChromeOptions opts = new ChromeOptions();
        opts.setHeadless(true);
        opts.addArguments("--no-sandbox");
        return new ChromeDriver(opts);
    }
}