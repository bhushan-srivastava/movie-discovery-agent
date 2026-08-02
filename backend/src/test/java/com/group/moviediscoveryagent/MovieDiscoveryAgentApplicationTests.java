package com.group.moviediscoveryagent;

import com.group.moviediscoveryagent.config.TestChatClientConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(TestChatClientConfig.class)
class MovieDiscoveryAgentApplicationTests {

	@Test
	void contextLoads() {
	}

}
