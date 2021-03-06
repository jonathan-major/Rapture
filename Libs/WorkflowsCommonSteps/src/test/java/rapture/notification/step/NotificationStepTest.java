/**
 * The MIT License (MIT)
 *
 * Copyright (c) 2011-2016 Incapture Technologies LLC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package rapture.notification.step;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import rapture.common.CallingContext;
import rapture.common.CreateResponse;
import rapture.common.RaptureConstants;
import rapture.common.RaptureURI;
import rapture.common.Scheme;
import rapture.common.WorkOrderExecutionState;
import rapture.common.dp.Step;
import rapture.common.dp.StepRecord;
import rapture.common.dp.StepRecordDebug;
import rapture.common.dp.Steps;
import rapture.common.dp.WorkOrderDebug;
import rapture.common.dp.WorkerDebug;
import rapture.common.dp.Workflow;
import rapture.common.exception.RaptureException;
import rapture.common.impl.jackson.JacksonUtil;
import rapture.common.model.RaptureExchange;
import rapture.common.model.RaptureExchangeQueue;
import rapture.common.model.RaptureExchangeType;
import rapture.config.ConfigLoader;
import rapture.config.RaptureConfig;
import rapture.kernel.ContextFactory;
import rapture.kernel.Kernel;
import rapture.kernel.script.KernelScript;
import rapture.mail.Mailer;
import rapture.mail.SMTPConfig;

public class NotificationStepTest {

    static String saveInitSysConfig;
    static String saveRaptureRepo;
    private static final String auth = "test" + System.currentTimeMillis();
    private static final String REPO_USING_MEMORY = "REP {} USING MEMORY {prefix=\"/tmp/" + auth + "\"}";
    static CallingContext context;
    static final String templateName = "TESTING";

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        context = ContextFactory.getKernelUser();
        RaptureConfig.setLoadYaml(false);
        RaptureConfig config = ConfigLoader.getConf();
        saveRaptureRepo = config.RaptureRepo;
        saveInitSysConfig = config.InitSysConfig;

        config.RaptureRepo = REPO_USING_MEMORY;
        config.InitSysConfig = "NREP {} USING MEMORY { prefix=\"/tmp/" + auth + "/sys.config\"}";

        System.setProperty("LOGSTASH-ISENABLED", "false");
        Kernel.initBootstrap();

        Kernel.INSTANCE.clearRepoCache(false);
        Kernel.getAudit().createAuditLog(ContextFactory.getKernelUser(), new RaptureURI(RaptureConstants.DEFAULT_AUDIT_URI, Scheme.LOG).getAuthority(),
                "LOG {} using MEMORY {prefix=\"/tmp/" + auth + "\"}");
        Kernel.getLock().createLockManager(ContextFactory.getKernelUser(), "lock://kernel", "LOCKING USING DUMMY {}", "");
        Kernel.getIdGen().createIdGen(context, "idgen://sys/dp/workOrder", "IDGEN {} USING MEMORY {}");
        Kernel.getIdGen().createIdGen(context, "idgen://sys/activity/id", "IDGEN {} USING MEMORY {}");

        try {
            Kernel.INSTANCE.restart();
            Kernel.initBootstrap(null, null, true);

            Kernel.getPipeline().getTrusted().registerServerCategory(context, "alpha", "Primary servers");
            Kernel.getPipeline().getTrusted().registerServerCategory(context, "beta", "Secondary servers");

            Kernel.getPipeline().registerExchangeDomain(context, "//main", "EXCHANGE {} USING MEMORY {}");

            RaptureExchange exchange = new RaptureExchange();
            exchange.setName("kernel");
            exchange.setName("kernel");
            exchange.setExchangeType(RaptureExchangeType.FANOUT);
            exchange.setDomain("main");

            List<RaptureExchangeQueue> queues = new ArrayList<>();
            RaptureExchangeQueue queue = new RaptureExchangeQueue();
            queue.setName("default");
            queue.setRouteBindings(new ArrayList<String>());
            queues.add(queue);

            exchange.setQueueBindings(queues);

            Kernel.getPipeline().getTrusted().registerPipelineExchange(context, "kernel", exchange);
            Kernel.getPipeline().getTrusted().bindPipeline(context, "alpha", "kernel", "default");

            // Now that the binding is setup, register our server as being part of "alpha"

            Kernel.setCategoryMembership("alpha");

            KernelScript ks = new KernelScript();
            ks.setCallingContext(context);

        } catch (RaptureException e) {
            e.printStackTrace();
        }
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    // Fill in for testing and enable testNotificationEmailStep
	String username = "";
	String password = "";
	String host = "";
    int port = 587;

    @Before
    public void setUp() throws Exception {
        // create smtp config
        String area = "CONFIG";
        CallingContext anon = ContextFactory.getAnonymousUser();
        SMTPConfig smtpConfig = new SMTPConfig().setFrom("support@incapturetechnologies.com").setUsername(username).setPassword(password).setHost(host)
                .setPort(port).setAuthentication(!StringUtils.isEmpty(password)).setTlsenable(!StringUtils.isEmpty(password))
                .setTlsrequired(!StringUtils.isEmpty(password));
        Kernel.getSys().writeSystemConfig(context, area, Mailer.SMTP_CONFIG_URL, JacksonUtil.jsonFromObject(smtpConfig));

        // create dummy email template
        String template = "{\"emailTo\":\"support@incapturetechnologies.com\",\"subject\":\"Ignore this message\",\"msgBody\":\"This email is generated from NotificationStepTest in WorkflowCommonSteps\"}";
        String url = Mailer.EMAIL_TEMPLATE_DIR + templateName;
        Kernel.getSys().writeSystemConfig(context, area, url, template);
    }

    @After
    public void tearDown() throws Exception {
    }

    // Enable this when we have a SMTP config that Google doesn't keep blocking
    @Ignore
    @Test
    public void testNotificationEmailStep() {

        String workflowUri = "workflow://foo/bar/baz";
        Workflow w = new Workflow();
        w.setStartStep("step1");
        List<Step> steps = new LinkedList<>();
        Step step = new Step();
        step.setExecutable("dp_java_invocable://notification.steps.NotificationStep");
        step.setName("step1");
        step.setDescription("description");
        steps.add(step);
        Map<String, String> viewMap = new HashMap<>();
        viewMap.put("NOTIFY_TYPE", "#" + "EMAIL");
        viewMap.put("MESSAGE_TEMPLATE", "#" + templateName);
        viewMap.put("RECIPIENT", "#" + "dave.tong@incapturetechnologies.com");
        w.setSteps(steps);
        w.setView(viewMap);
        w.setWorkflowURI(workflowUri);
        Kernel.getDecision().putWorkflow(context, w);

        CreateResponse response = Kernel.getDecision().createWorkOrderP(context, workflowUri, null, null);
        assertTrue(response.getIsCreated());
        WorkOrderDebug debug;
        WorkOrderExecutionState state = WorkOrderExecutionState.NEW;
        long timeout = System.currentTimeMillis() + 60000;
        do {
            debug = Kernel.getDecision().getWorkOrderDebug(context, response.getUri());
            state = debug.getOrder().getStatus();
        } while (((state == WorkOrderExecutionState.NEW) || (state == WorkOrderExecutionState.ACTIVE)) && (System.currentTimeMillis() < timeout));

        StepRecord sr = null;
        for (WorkerDebug wd : debug.getWorkerDebugs()) {
            for (StepRecordDebug srd : wd.getStepRecordDebugs()) {
                sr = srd.getStepRecord();
                System.out.println(JacksonUtil.formattedJsonFromObject(sr));
            }
        }
        assertNotNull(sr);
        assertEquals(sr.toString(), Steps.NEXT.toString(), sr.getRetVal());
        assertEquals(WorkOrderExecutionState.FINISHED, debug.getOrder().getStatus());
    }

    @Test
    public void testNotificationSlackStep() {

        String workflowUri = "workflow://foo/bar/baz";
        Workflow w = new Workflow();
        w.setStartStep("step1");
        List<Step> steps = new LinkedList<>();
        Step step = new Step();
        step.setExecutable("dp_java_invocable://notification.steps.NotificationStep");
        step.setName("step1");
        step.setDescription("description");
        steps.add(step);
        Map<String, String> viewMap = new HashMap<>();
        viewMap.put("MESSAGE_TEMPLATE", "#" + templateName);
        viewMap.put("RECIPIENT", "#" + "dave.tong@incapturetechnologies.com");
        w.setSteps(steps);
        w.setWorkflowURI(workflowUri);
        viewMap.put("RECIPIENT", "#" + "https://hooks.slack.com/services/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");
        viewMap.put("NOTIFY_TYPE", "#" + "SLACK");
        w.setView(viewMap);
        Kernel.getDecision().putWorkflow(context, w);

        CreateResponse response = Kernel.getDecision().createWorkOrderP(context, workflowUri, null, null);
        assertTrue(response.getIsCreated());
        WorkOrderExecutionState state = WorkOrderExecutionState.NEW;
        long timeout = System.currentTimeMillis() + 60000;
        WorkOrderDebug debug;
        do {
            debug = Kernel.getDecision().getWorkOrderDebug(context, response.getUri());
            state = debug.getOrder().getStatus();
        } while (((state == WorkOrderExecutionState.NEW) || (state == WorkOrderExecutionState.ACTIVE)) && (System.currentTimeMillis() < timeout));

        StepRecord sr = debug.getWorkerDebugs().get(0).getStepRecordDebugs().get(0).getStepRecord();
        // TODO
        // assertEquals(Steps.NEXT.toString(), sr.getRetVal());
        assertEquals(WorkOrderExecutionState.FINISHED, debug.getOrder().getStatus());

    }

}
