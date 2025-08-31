package org.jd.core.v1;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.message.MapMessage;
import org.apache.logging.log4j.util.TriConsumer;

import java.util.Collections;
import java.util.List;

public class MiscOracleJDK8 {
	class Consumer {
		void accept(LogEvent event, TriConsumer<String, Object, StringBuilder> mapWriter, StringBuilder builder) {
			if (event.getMessage() instanceof MapMessage) {
				((MapMessage<?, ?>) event.getMessage()).forEach((key, value) -> mapWriter.accept(key, value, builder));
			}
		}
	}

	class LambdaVariables {
		@SuppressWarnings("unused")
		void test(String str, int intger) {
			char chrctr = Character.MAX_VALUE;
			CharSequence chrsq = null;
			List<Integer> lst = null;
			Runnable r = (() -> {
				Collections.sort(lst, (a, b) -> {
					System.out.print(intger);
					System.out.print(chrsq);
					System.out.print(str);
					System.out.print(lst);
					System.out.print(chrctr);
					return Integer.compare(a, b);
				});
			});
		}
	}
}
