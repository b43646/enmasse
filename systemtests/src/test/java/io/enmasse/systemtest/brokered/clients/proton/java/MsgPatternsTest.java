/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.brokered.clients.proton.java;

import io.enmasse.systemtest.ArtemisManagement;
import io.enmasse.systemtest.ability.ITestBaseBrokered;
import io.enmasse.systemtest.bases.clients.ClientTestBase;
import io.enmasse.systemtest.messagingclients.proton.java.ProtonJMSClientReceiver;
import io.enmasse.systemtest.messagingclients.proton.java.ProtonJMSClientSender;
import io.enmasse.systemtest.resolvers.ArtemisManagementParameterResolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(ArtemisManagementParameterResolver.class)
class MsgPatternsTest extends ClientTestBase implements ITestBaseBrokered {

    @Test
    void testBasicMessage() throws Exception {
        doBasicMessageTest(new ProtonJMSClientSender(logPath), new ProtonJMSClientReceiver(logPath));
    }

    @Test
    void testRoundRobinReceiver(ArtemisManagement artemisManagement) throws Exception {
        doRoundRobinReceiverTest(artemisManagement, new ProtonJMSClientSender(logPath), new ProtonJMSClientReceiver(logPath), new ProtonJMSClientReceiver(logPath));
    }

    @Test
    void testTopicSubscribe(ArtemisManagement artemisManagement) throws Exception {
        doTopicSubscribeTest(artemisManagement, new ProtonJMSClientSender(logPath), new ProtonJMSClientReceiver(logPath), new ProtonJMSClientReceiver(logPath), true);
    }

    @Test
    void testMessageBrowse() throws Exception {
        doMessageBrowseTest(new ProtonJMSClientSender(logPath), new ProtonJMSClientReceiver(logPath), new ProtonJMSClientReceiver(logPath));
    }

    @Test
    void testDrainQueue() throws Exception {
        doDrainQueueTest(new ProtonJMSClientSender(logPath), new ProtonJMSClientReceiver(logPath));
    }

    @Test
    void testMessageSelectorQueue() throws Exception {
        doMessageSelectorQueueTest(new ProtonJMSClientSender(logPath), new ProtonJMSClientReceiver(logPath));
    }

    @Test
    void testMessageSelectorTopic(ArtemisManagement artemisManagement) throws Exception {
        doMessageSelectorTopicTest(artemisManagement, new ProtonJMSClientSender(logPath), new ProtonJMSClientReceiver(logPath),
                new ProtonJMSClientReceiver(logPath), new ProtonJMSClientReceiver(logPath), true);
    }
}
