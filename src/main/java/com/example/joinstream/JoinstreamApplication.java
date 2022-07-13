package com.example.joinstream;

import io.confluent.kafka.streams.serdes.avro.GenericAvroSerde;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.*;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;

@SpringBootApplication
@Slf4j
public class JoinstreamApplication {

	public static void main(String[] args) {
		SpringApplication.run(JoinstreamApplication.class, args);
		log.info("Joinstream application started.");
	}

	@Bean
	public KStream<String, GenericRecord> kStream (StreamsBuilder kStreamBuilder) {

		GenericAvroSerde genericAvroSerde = new GenericAvroSerde();
		Map<String, String> serdeConfig = Collections.singletonMap("schema.registry.url", "http://localhost:8081");
		genericAvroSerde.configure(serdeConfig, true);

		KStream<String, GenericRecord> stream1 = kStreamBuilder.stream("CustomerReKeyed");
		KStream<String, GenericRecord> stream2 = kStreamBuilder.stream("Balance");
		KStream<String, GenericRecord> joined = stream1.join(
				stream2,
				new joinCustomerBalanceMapper(),
				JoinWindows.of(Duration.ofMinutes(5)),
				StreamJoined.with(
						Serdes.String(),
						genericAvroSerde,
						genericAvroSerde)
		);
		joined.to("CustomerBalance");
		joined.print(Printed.toSysOut());
		return joined;
	}

	public static class joinCustomerBalanceMapper implements ValueJoiner<GenericRecord, GenericRecord ,GenericRecord>{


		@Override
		public GenericRecord apply(GenericRecord customer, GenericRecord balance) {

			log.info(customer.toString());
			log.info(balance.toString());

			Schema schema = buildOutputSchema();
			GenericRecord customerBalanceRecord = new GenericData.Record(schema);
			customerBalanceRecord.put("accountId", customer.get("accountId"));
			customerBalanceRecord.put("customerId", customer.get("customerId"));
			customerBalanceRecord.put("phoneNumber", customer.get("customerId"));
			customerBalanceRecord.put("balance", balance.get("balance"));

			return customerBalanceRecord;
		}

		private Schema buildOutputSchema() {

			String customerBalanceSchema = "{\n" +
					"    \"namespace\": \"com.ibm.gbs.schema\",\n" +
					"    \"type\": \"record\",\n" +
					"    \"name\": \"CustomerBalance\",\n" +
					"    \"fields\": [\n" +
					"        {\n" +
					"            \"name\": \"accountId\",\n" +
					"            \"type\": {\n" +
					"                \"avro.java.string\": \"String\",\n" +
					"                \"type\": \"string\"\n" +
					"                }\n" +
					"        }\n" +
					"        ,\n" +
					"        {\n" +
					"            \"name\": \"customerId\",\n" +
					"            \"type\": {\n" +
					"                \"avro.java.string\": \"String\",\n" +
					"                \"type\": \"string\"\n" +
					"                }\n" +
					"        },\n" +
					"        {\n" +
					"            \"name\": \"phoneNumber\",\n" +
					"            \"type\": {\n" +
					"                \"avro.java.string\": \"String\",\n" +
					"                \"type\": \"string\"\n" +
					"                }\n" +
					"        },\n" +
					"        {\n" +
					"            \"name\": \"balance\",\n" +
					"            \"type\": \"float\"\n" +
					"        }\n" +
					"    ]\n" +
					"}";

			return new Schema.Parser().parse(customerBalanceSchema);
		}
	}

}
