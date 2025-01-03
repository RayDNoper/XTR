package ee.buerokratt.xtr.services;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.apache.logging.log4j.message.ParameterizedMessage.deepToString;

@Slf4j
@Service
public class HandlebarsHelper {

    @Value("application.xroad-instance")
    String xroadInstance;

    @Value("application.client-data.member-class")
    String memberClass;

    @Value("application.client-data.member-code")
    String memberCode;

    @Value("application.client-data.subsystem-code")
    String subsystemCode;

    public String apply(String template, Map<String, String> values) throws IOException {
        Handlebars hbs = new Handlebars();
        Template result = hbs.compileInline(template);

        Map<String, String> localValues = new HashMap<>();
        localValues.put("generate_uuid", generateUUID());
        localValues.put("generate_client", generateClientEnvelope());
        localValues.put("generate_instance", xroadInstance);

        localValues.putAll(values);

        log.info("Local values: "+deepToString(localValues));

        return result
                .apply(localValues);
    }

    private String generateClientEnvelope() {
        return ("<xroad:client id:objectType='SUBSYSTEM'>" +
                "<id:xRoadInstance>%s</id:xRoadInstance>" +
                "<id:memberClass>%s</id:memberClass>" +
                "<id:memberCode>%s</id:memberCode>" +
                "<id:subsystemCode>%s</id:subsystemCode>" +
                "</xroad:client>")
                        .formatted(
                        xroadInstance,
                        memberClass,
                        memberCode,
                        subsystemCode);
    }

    private String generateUUID() {
        return UUID.randomUUID().toString();
    }
}
