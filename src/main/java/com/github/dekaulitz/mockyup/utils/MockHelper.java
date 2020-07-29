package com.github.dekaulitz.mockyup.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.github.dekaulitz.mockyup.domain.mocks.vmodels.DtoMockResponseVmodel;
import com.github.dekaulitz.mockyup.infrastructure.errors.handlers.InvalidMockException;
import io.swagger.util.Json;
import io.swagger.v3.oas.models.Components;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.http.HttpMethod;

import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@NoArgsConstructor
/**
 * mock helper for handling mock extesion from openapi
 */
public class MockHelper {
    public static final String X_PATH = "x-path-including";
    public final static String NAME_PROPERTY = "name";
    public final static String VALUE_PROPERTY = "value";
    public static final String X_EXAMPLES = "x-examples";
    public static final String X_HEADERS = "x-header-including";
    public static final String X_QUERY = "x-query-including";
    public static final String X_BODY = "x-body-including";
    public static final String X_DEFAULT = "x-default";

    public final static String PROPERTY = "property";
    public final static String RESPONSE = "response";
    @Getter
    @Setter
    private Map<String, Object> property;
    @Setter
    @Getter
    private DtoMockResponseVmodel response;

    /**
     * @param request
     * @param extension
     * @param components
     * @return
     * @desc generate mock response base on header
     */
    public static MockHelper generateResponseHeader(HttpServletRequest request, List<Map<String, Object>> extension, Components components) throws InvalidMockException {
        for (Map<String, Object> stringObjectMap : extension) {
            try {
                MockHelper mockHelper = new MockHelper();
                parsingMockFromJsonMapper(mockHelper, stringObjectMap);
                throwInvalidMockExample(mockHelper);
                String requestHeader = request.getHeader((String) mockHelper.getProperty().get(MockHelper.NAME_PROPERTY));
                if (!requestHeader.isEmpty()) {
                    if (requestHeader.equals(mockHelper.getProperty().get(MockHelper.VALUE_PROPERTY))) {
                        getComponentReference(mockHelper, components);
                        return mockHelper;
                    }
                } else {
                    if (mockHelper.getProperty().get(VALUE_PROPERTY) == null) {
                        getComponentReference(mockHelper, components);
                        return mockHelper;
                    }
                }
            } catch (Exception e) {
                throw new InvalidMockException(ResponseCode.INVALID_MOCK_HEADER, e);
            }
        }
        return null;
    }

    /**
     * @param request
     * @param extension
     * @param body
     * @param components
     * @return
     * @throws JsonProcessingException
     * @desc generate mock response base on body
     */
    public static MockHelper generateResponseBody(HttpServletRequest request, List<Map<String, Object>> extension, String body, Components components) throws JsonProcessingException, InvalidMockException {
        if (request.getMethod().equals(HttpMethod.POST.toString()) || request.getMethod().equals(HttpMethod.PATCH.toString())
                || request.getMethod().equals(HttpMethod.PUT.toString()))
            for (Map<String, Object> stringObjectMap : extension) {
                JsonNode requestBody = Json.mapper().readTree(body);
                MockHelper mockHelper = new MockHelper();
                parsingMockFromJsonMapper(mockHelper, stringObjectMap);
                throwInvalidMockExample(mockHelper);
                boolean isBodyExist = requestBody.has((String) mockHelper.getProperty().get(MockHelper.NAME_PROPERTY));
                if (isBodyExist) {
                    String bodyRequest = requestBody.findPath((String) mockHelper.getProperty().get(MockHelper.NAME_PROPERTY)).asText();
                    if (bodyRequest != null) {
                        if (!bodyRequest.isEmpty()) {
                            if (bodyRequest.equals(mockHelper.getProperty().get(MockHelper.VALUE_PROPERTY))) {
                                getComponentReference(mockHelper, components);
                                return mockHelper;
                            }
                            if (bodyRequest.equalsIgnoreCase("null") && mockHelper.getProperty().get(VALUE_PROPERTY) == null) {
                                getComponentReference(mockHelper, components);
                                return mockHelper;
                            }
                        }
                    }
                } else if (mockHelper.getProperty().get(VALUE_PROPERTY) == null) {
                    getComponentReference(mockHelper, components);
                    return mockHelper;
                }
            }
        return null;
    }

    /**
     * @param extension
     * @param openAPIPaths
     * @param paths
     * @param components
     * @return
     * @desc generate mock response base on path
     */
    public static MockHelper generateResponsePath(List<Map<String, Object>> extension, String[] openAPIPaths, String[] paths, Components components) throws InvalidMockException {
        for (Map<String, Object> stringObjectMap : extension) {
            MockHelper mockHelper = new MockHelper();
            parsingMockFromJsonMapper(mockHelper, stringObjectMap);

            throwInvalidMockExample(mockHelper);
            for (int i = 0; i < openAPIPaths.length; i++) {
                if (!openAPIPaths[i].equals(paths[i])) {
                    if (openAPIPaths[i].contains((CharSequence) mockHelper.getProperty().get(MockHelper.NAME_PROPERTY))) {
                        if (paths[i].equals(mockHelper.getProperty().get(MockHelper.VALUE_PROPERTY).toString())) {
                            getComponentReference(mockHelper, components);
                            return mockHelper;
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * @param request
     * @param extension
     * @param components
     * @return
     * @desc generate response query base on query
     */
    public static MockHelper generateResponseQuery(HttpServletRequest request, List<Map<String, Object>> extension, Components components) throws InvalidMockException, UnsupportedEncodingException {
        //for query mock response for the ordering its depend on your mock query-including
        //if youre first query was a=x&b=y but from the query mock struct b=y its defined first so it will throwing response from b=y
        for (Map<String, Object> stringObjectMap : extension) {
            MockHelper mockHelper = new MockHelper();
            parsingMockFromJsonMapper(mockHelper, stringObjectMap);
            throwInvalidMockExample(mockHelper);
            String cleanQueryString = java.net.URLDecoder.decode(request.getQueryString(), String.valueOf(StandardCharsets.UTF_8));
            String[] queryStrings = cleanQueryString.split("\\?");
            if (queryStrings.length > 1) {
                Map<String, String> q = decodeQueryString(queryStrings[1]);
                for (Map.Entry<String, String> qmap : q.entrySet()) {
                    if ((qmap.getKey().equals(mockHelper.getProperty().get(MockHelper.NAME_PROPERTY)))) {
                        if ((qmap.getValue().equals(mockHelper.getProperty().get(MockHelper.VALUE_PROPERTY)))) {
                            getComponentReference(mockHelper, components);
                            return mockHelper;
                        }
                        if (mockHelper.getProperty().get(VALUE_PROPERTY) == null && qmap.getValue().equals("null")) {
                            getComponentReference(mockHelper, components);
                            return mockHelper;
                        }
                    } else {
                        if (mockHelper.getProperty().get(VALUE_PROPERTY) == null &&
                                !cleanQueryString.contains(mockHelper.getProperty().get(MockHelper.NAME_PROPERTY).toString())) {
                            getComponentReference(mockHelper, components);
                            return mockHelper;
                        }
                    }
                }
            } else {
                if (mockHelper.getProperty().get(VALUE_PROPERTY) == null) {
                    getComponentReference(mockHelper, components);
                    return mockHelper;
                } else {
                    if (cleanQueryString.contains(mockHelper.getProperty().get(MockHelper.NAME_PROPERTY) + "=" + mockHelper.getProperty().get(MockHelper.VALUE_PROPERTY))) {
                        getComponentReference(mockHelper, components);
                        return mockHelper;
                    }
                }
            }

        }
        return null;
    }

    /**
     * @param extension
     * @param components
     * @return
     * @desc generate default response
     */
    public static MockHelper generateResponseDefault(Map<String, Object> extension, Components components) throws InvalidMockException {
        MockHelper mockHelper = new MockHelper();
        parsingMockFromJsonMapper(mockHelper, extension);
        if (mockHelper.getResponse() == null) {
            throw new InvalidMockException(ResponseCode.INVALID_MOCK_DEFAULT);
        }
        getComponentReference(mockHelper, components);
        return mockHelper;
    }

    private static void parsingMockFromJsonMapper(MockHelper mockHelper, Map<String, Object> xExamples) {
        mockHelper.setProperty(JsonMapper.mapper().convertValue(xExamples.get(PROPERTY), Map.class));
        mockHelper.setResponse(JsonMapper.mapper().convertValue(xExamples.get(RESPONSE), DtoMockResponseVmodel.class));
    }

    private static void getComponentReference(MockHelper mockHelper, Components components) throws InvalidMockException {
        if (mockHelper.getResponse().get$ref() != null) {
            String refName = OpenAPITools.getSimpleRef((String) mockHelper.getResponse().get$ref());
            if (!refName.isEmpty()) {
                try {
                    mockHelper.getResponse().setResponse(components.getExamples().get(refName).getValue());
                } catch (NullPointerException n) {
                    throw new InvalidMockException(ResponseCode.INVALID_MOCK_REF);
                }
            }

        }
    }

    private static Map<String, String> decodeQueryString(String query) throws InvalidMockException {
        String[] params = query.split("\\&");
        Map<String, String> map = new HashMap<>();
        String value = "";
        String name = "";
        for (String param : params) {
            if (param.split("=").length <= 1) {
                name = param.split("=")[0];
                value = "null";
            } else {
                name = param.split("=")[0];
                value = param.split("=")[1];
            }
            map.put(name, value);
        }

        return map;
    }

    private static void throwInvalidMockExample(MockHelper mockHelper) throws InvalidMockException {
        if (mockHelper.getResponse() == null || mockHelper.getProperty() == null) {
            throw new InvalidMockException(ResponseCode.INVALID_MOCKUP_STRUCTURE);
        }
    }
}
