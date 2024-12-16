package ee.buerokratt.xtr.controllers;

import ee.buerokratt.xtr.domain.YamlXRoadTemplate;
import ee.buerokratt.xtr.services.RequestExecutorService;
import ee.buerokratt.xtr.services.XRoadTemplatesService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;

import static org.apache.logging.log4j.message.ParameterizedMessage.deepToString;

@Slf4j
@RestController
@RequestMapping(path = "**", consumes = {MediaType.APPLICATION_JSON_VALUE})
public class XRoadRequestController {
    @Autowired
    XRoadTemplatesService serviceReader;

    @Autowired
    RequestExecutorService executor;

    @PostMapping
    public ResponseEntity<Object> requestXRoad(@RequestBody(required = false) Map<String, String> requestBody,
                                               @RequestHeader(required = false) Map<String, String> requestHeaders,
                                               HttpServletRequest request) {
        String[] uriParts = request.getRequestURI().split("/");

        log.info("got request: " + deepToString(uriParts));

        YamlXRoadTemplate service = serviceReader.getService(uriParts[1], uriParts[2]);

        log.info("loaded template: " + deepToString( service ));

        try {
	    return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON)
            	.body(executor.execute(service, requestBody));
        } catch (IOException e) {
            return ResponseEntity.badRequest().body(e.getCause());
        }
    }
}
