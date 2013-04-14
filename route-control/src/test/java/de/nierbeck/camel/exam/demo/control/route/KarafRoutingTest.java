package de.nierbeck.camel.exam.demo.control.route;

import static org.apache.karaf.tooling.exam.options.KarafDistributionOption.karafDistributionConfiguration;
import static org.apache.karaf.tooling.exam.options.KarafDistributionOption.keepRuntimeFolder;
import static org.apache.karaf.tooling.exam.options.KarafDistributionOption.logLevel;
import static org.ops4j.pax.exam.CoreOptions.maven;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.scanFeatures;
import static org.ops4j.pax.exam.CoreOptions.streamBundle;
import static org.ops4j.pax.tinybundles.core.TinyBundles.bundle;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.util.Dictionary;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.jms.ConnectionFactory;
import javax.sql.DataSource;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.apache.felix.service.command.CommandProcessor;
import org.apache.felix.service.command.CommandSession;
import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.tooling.exam.options.KarafDistributionOption;
import org.apache.karaf.tooling.exam.options.LogLevelOption.LogLevel;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.TestProbeBuilder;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.ExamReactorStrategy;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.exam.junit.ProbeBuilder;
import org.ops4j.pax.exam.spi.reactors.AllConfinedStagedReactorFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.nierbeck.camel.exam.demo.control.CamelMessageBean;
import de.nierbeck.camel.exam.demo.control.JmsDestinations;
import de.nierbeck.camel.exam.demo.control.RouteID;
import de.nierbeck.camel.exam.demo.control.WebServiceOrder;
import de.nierbeck.camel.exam.demo.control.WebServiceOrderImpl;
import de.nierbeck.camel.exam.demo.control.internal.OrderWebServiceRoute;
import de.nierbeck.camel.exam.demo.control.internal.OutMessageProcessor;
import de.nierbeck.camel.exam.demo.control.internal.converter.MessageLogConverter;
import de.nierbeck.camel.exam.demo.entities.CamelMessage;
import de.nierbeck.camel.exam.demo.entities.dao.CamelMessageStoreDao;
import de.nierbeck.camel.exam.demo.testutil.TestUtility;

@RunWith(JUnit4TestRunner.class)
//@ExamReactorStrategy(EagerSingleStagedReactorFactory.class)
@ExamReactorStrategy(AllConfinedStagedReactorFactory.class)
public class KarafRoutingTest extends CamelTestSupport {

	protected transient Logger log = LoggerFactory.getLogger(getClass());

	ExecutorService executor = Executors.newCachedThreadPool();

	static final Long COMMAND_TIMEOUT = 10000L;
	static final Long DEFAULT_TIMEOUT = 20000L;
	static final Long SERVICE_TIMEOUT = 30000L;

	@Inject
	protected FeaturesService featuresService;

	@Inject
	protected BundleContext bundleContext;

	@Inject
	protected DataSource dataSource;

	@Inject
	protected CamelMessageStoreDao orderMergingDao;

	@Inject
	private ConnectionFactory connectionFactory;

	private CamelContext controlContext;

	private CamelContext testContext;

	@Configuration
	public static Option[] configure() throws Exception {
		return new Option[] {
				karafDistributionConfiguration()
						.frameworkUrl(
								maven().groupId("org.apache.karaf").artifactId("apache-karaf").type("zip")
										.versionAsInProject()).useDeployFolder(false).karafVersion("2.2.9")
						.unpackDirectory(new File("target/paxexam/unpack/")),
				scanFeatures(
						maven().groupId("org.apache.karaf.assemblies.features").artifactId("standard").type("xml")
								.classifier("features").versionAsInProject(), "http-whiteboard").start(),
				scanFeatures(
						maven().groupId("org.apache.karaf.assemblies.features").artifactId("enterprise").type("xml")
								.classifier("features").versionAsInProject(), "transaction", "jpa", "jndi").start(),
				scanFeatures(
						maven().groupId("org.apache.activemq").artifactId("activemq-karaf").type("xml")
								.classifier("features").versionAsInProject(), "activemq-blueprint", "activemq-camel")
						.start(),
				scanFeatures(
						maven().groupId("org.apache.cxf.karaf").artifactId("apache-cxf").type("xml")
								.classifier("features").versionAsInProject(), "cxf-jaxws").start(),
				scanFeatures(
						maven().groupId("org.apache.camel.karaf").artifactId("apache-camel").type("xml")
								.classifier("features").versionAsInProject(), "camel-blueprint", "camel-jms",
						"camel-jpa", "camel-mvel", "camel-jdbc", "camel-cxf", "camel-test").start(),
				logLevel(LogLevel.INFO),
				KarafDistributionOption.editConfigurationFilePut("etc/org.ops4j.pax.url.mvn.cfg",
						"org.ops4j.pax.url.mvn.proxySupport", "true"),
				keepRuntimeFolder(),
				
				mavenBundle().groupId("com.h2database").artifactId("h2").version("1.3.167"),
				mavenBundle().groupId("de.nierbeck.camel.exam.demo").artifactId("entities").versionAsInProject(),
				mavenBundle().groupId("org.ops4j.pax.tipi").artifactId("org.ops4j.pax.tipi.hamcrest.core")
						.versionAsInProject(),
				streamBundle(
						bundle().add("OSGI-INF/blueprint/datasource.xml",
								new File("src/sample/resources/datasource.xml").toURL())
								.set(Constants.BUNDLE_SYMBOLICNAME, "de.nierbeck.camel.exam.demo.datasource")
								.set(Constants.DYNAMICIMPORT_PACKAGE, "*").build()).start(),
				streamBundle(
						bundle().add("OSGI-INF/blueprint/mqbroker.xml",
								new File("src/sample/resources/mqbroker-test.xml").toURL())
								.set(Constants.BUNDLE_SYMBOLICNAME, "de.nierbeck.camel.exam.demo.broker")
								.set(Constants.DYNAMICIMPORT_PACKAGE, "*").build()).start(),
				streamBundle(
						bundle().add(JmsDestinations.class)
								.add(WebServiceOrder.class)
								.add(CamelMessageBean.class)
								.add(WebServiceOrderImpl.class)
								.add(RouteID.class)
								.add(OrderWebServiceRoute.class)
								.add(OutMessageProcessor.class)
								.add(MessageLogConverter.class)
								.add("OSGI-INF/blueprint/camel-main-context.xml",
										new File("src/main/resources/OSGI-INF/blueprint/camel-context.xml")
												.toURL())
								.add("OSGI-INF/blueprint/jms-context.xml",
										new File("src/main/resources/OSGI-INF/blueprint/jms-config.xml").toURL())
								.add("wsdl/WebServiceOrder.wsdl",
										new File("target/generated/wsdl/WebServiceOrder.wsdl").toURL())
								.set(Constants.BUNDLE_SYMBOLICNAME, "de.nierbeck.camel.exam.demo.route-control")
								.set(Constants.DYNAMICIMPORT_PACKAGE, "*")
								.set(Constants.EXPORT_PACKAGE, "wsdl, de.nierbeck.camel.exam.demo.control").build())
						.start() };
	}

	/**
	 * @param probe
	 * @return
	 */
	@ProbeBuilder
	public TestProbeBuilder probeConfiguration(TestProbeBuilder probe) {
		// makes sure the generated Test-Bundle contains this import!
		probe.setHeader(Constants.BUNDLE_SYMBOLICNAME, "de.nierbeck.camel.exam.demo.route-control-test");
		probe.setHeader(Constants.DYNAMICIMPORT_PACKAGE, "de.nierbeck.camel.exam.demo.control,*,org.apache.felix.service.*;status=provisional");
		return probe;
	}

	@Override
	public boolean isCreateCamelContextPerClass() {
		// we override this method and return true, to tell Camel test-kit that
		// it should only create CamelContext once (per class), so we will
		// re-use the CamelContext between each test method in this class
		return true;
	}

	@Override
	protected void doPreSetup() throws Exception {
		controlContext = getOsgiService(CamelContext.class, "(camel.context.name=route-control)", 10000);
		assertNotNull(controlContext);

		testContext = getOsgiService(CamelContext.class, "(camel.context.name=route-test)", 10000);
		assertNotNull(testContext);

		for (CamelMessage orderMerging : orderMergingDao.findAll()) {
			orderMergingDao.makeTransient(orderMerging);
		}

	}
	
	@Test
	public void test() throws Exception {
		assertTrue(featuresService.isInstalled(featuresService.getFeature("camel-jpa")));
		assertTrue(featuresService.isInstalled(featuresService.getFeature("camel-core")));
		assertTrue(featuresService.isInstalled(featuresService.getFeature("camel-blueprint")));
		assertTrue(featuresService.isInstalled(featuresService.getFeature("activemq-camel")));
		assertTrue(featuresService.isInstalled(featuresService.getFeature("http-whiteboard")));

		// Use these for debugging when test doesn't work right.
//		System.err.println(executeCommand("features:list"));
//		System.err.println(executeCommand("camel:route-list"));
//		System.err.println(executeCommand("list"));

		String command = executeCommand("camel:context-list");
		System.err.println(command);
		assertTrue("Doesn't contain desired camel-contexts", command.contains("route-control"));
		assertTrue("Doesn't contain desired camel-contexts", command.contains("route-test"));
		
	}

	@Test
	public final void testSendMessage() throws Exception {
		final CamelMessageBean body = new CamelMessageBean();
		body.setMessage("Testmessage");
		body.setTmstamp(Long.toString(System.currentTimeMillis()));

		MockEndpoint mockEndpoint = (MockEndpoint) testContext.getEndpoint("mock:OrderRoute");
		mockEndpoint.expectedMessageCount(1);

		ProducerTemplate template = testContext.createProducerTemplate();
		template.start();

		template.send("direct:start", new Processor() {
			public void process(Exchange exchange) {
				Message in = exchange.getIn();
				in.setBody(body);
			}
		});

		mockEndpoint.assertIsSatisfied(2500);
		
		/*
		Exchange exchange = mockEndpoint.getReceivedExchanges().get(0);
		Map mockBody = exchange.getIn().getBody(Map.class);
		assertNotNull(mockBody);

		assertEquals(body.getMessage(), mockBody.get(OrderConverter.BRANDID));
		assertEquals(body.getInputDir(), mockBody.get(OrderConverter.INPUTDIR));
		assertEquals(body.getSender(), mockBody.get(OrderConverter.SENDER));
		assertEquals(body.getTmstampAcceptance(), mockBody.get(OrderConverter.TMSTMP));
		assertNotNull(mockBody.get(OrderConverter.ORDERID));

		mockEndpoint.reset();

		long countAll = orderMergingDao.countAll();
		int c = 0;
		while (countAll == 0 && c < 5) {
			Thread.sleep(500);
			countAll = orderMergingDao.countAll();
			c++;
		}
		
		assertTrue(orderMergingDao.countAll() > 0);
		*/

	}

	// Below are methods used for testing --> should be moved outside of
	// testclass

	/**
	 * Executes a shell command and returns output as a String. Commands have a
	 * default timeout of 10 seconds.
	 * 
	 * @param command
	 * @return
	 */
	protected String executeCommand(final String command) {
		return executeCommand(command, COMMAND_TIMEOUT, false);
	}

	/**
	 * Executes a shell command and returns output as a String. Commands have a
	 * default timeout of 10 seconds.
	 * 
	 * @param command
	 *            The command to execute.
	 * @param timeout
	 *            The amount of time in millis to wait for the command to
	 *            execute.
	 * @param silent
	 *            Specifies if the command should be displayed in the screen.
	 * @return
	 */
	protected String executeCommand(final String command, final Long timeout, final Boolean silent) {
		String response;
		final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		final PrintStream printStream = new PrintStream(byteArrayOutputStream);
		final CommandProcessor commandProcessor = getOsgiService(CommandProcessor.class);
		final CommandSession commandSession = commandProcessor.createSession(System.in, printStream, System.err);
		FutureTask<String> commandFuture = new FutureTask<String>(new Callable<String>() {
			public String call() {
				try {
					if (!silent) {
						System.err.println(command);
					}
					commandSession.execute(command);
				} catch (Exception e) {
					e.printStackTrace(System.err);
				}
				printStream.flush();
				return byteArrayOutputStream.toString();
			}
		});

		try {
			executor.submit(commandFuture);
			response = commandFuture.get(timeout, TimeUnit.MILLISECONDS);
		} catch (Exception e) {
			e.printStackTrace(System.err);
			response = "SHELL COMMAND TIMED OUT: ";
		}

		return response;
	}

	/**
	 * Executes multiple commands inside a Single Session. Commands have a
	 * default timeout of 10 seconds.
	 * 
	 * @param commands
	 * @return
	 */
	protected String executeCommands(final String... commands) {
		String response;
		final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		final PrintStream printStream = new PrintStream(byteArrayOutputStream);
		final CommandProcessor commandProcessor = getOsgiService(CommandProcessor.class);
		final CommandSession commandSession = commandProcessor.createSession(System.in, printStream, System.err);
		FutureTask<String> commandFuture = new FutureTask<String>(new Callable<String>() {
			public String call() {
				try {
					for (String command : commands) {
						System.err.println(command);
						commandSession.execute(command);
					}
				} catch (Exception e) {
					e.printStackTrace(System.err);
				}
				return byteArrayOutputStream.toString();
			}
		});

		try {
			executor.submit(commandFuture);
			response = commandFuture.get(COMMAND_TIMEOUT, TimeUnit.MILLISECONDS);
		} catch (Exception e) {
			e.printStackTrace(System.err);
			response = "SHELL COMMAND TIMED OUT: ";
		}

		return response;
	}

	protected <T> T getOsgiService(Class<T> type, long timeout) {
		return getOsgiService(type, null, timeout);
	}

	protected <T> T getOsgiService(Class<T> type) {
		return getOsgiService(type, null, SERVICE_TIMEOUT);
	}

	protected <T> T getOsgiService(Class<T> type, String filter, long timeout) {
		ServiceTracker tracker = null;
		try {
			String flt;
			if (filter != null) {
				if (filter.startsWith("(")) {
					flt = "(&(" + Constants.OBJECTCLASS + "=" + type.getName() + ")" + filter + ")";
				} else {
					flt = "(&(" + Constants.OBJECTCLASS + "=" + type.getName() + ")(" + filter + "))";
				}
			} else {
				flt = "(" + Constants.OBJECTCLASS + "=" + type.getName() + ")";
			}
			Filter osgiFilter = FrameworkUtil.createFilter(flt);
			tracker = new ServiceTracker(bundleContext, osgiFilter, null);
			tracker.open(true);
			// Note that the tracker is not closed to keep the reference
			// This is buggy, as the service reference may change i think
			Object svc = type.cast(tracker.waitForService(timeout));
			if (svc == null) {
				Dictionary dic = bundleContext.getBundle().getHeaders();
				System.err.println("Test bundle headers: " + TestUtility.explode(dic));

				for (ServiceReference ref : TestUtility.asCollection(bundleContext.getAllServiceReferences(null, null))) {
					System.err.println("ServiceReference: " + ref);
				}

				for (ServiceReference ref : TestUtility.asCollection(bundleContext.getAllServiceReferences(null, flt))) {
					System.err.println("Filtered ServiceReference: " + ref);
				}

				throw new RuntimeException("Gave up waiting for service " + flt);
			}
			return type.cast(svc);
		} catch (InvalidSyntaxException e) {
			throw new IllegalArgumentException("Invalid filter", e);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

}
