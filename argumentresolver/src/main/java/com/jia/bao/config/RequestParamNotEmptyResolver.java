package com.jia.bao.config;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.core.GenericCollectionTypeResolver;
import org.springframework.core.MethodParameter;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ValueConstants;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.annotation.AbstractNamedValueMethodArgumentResolver;
import org.springframework.web.method.support.UriComponentsContributor;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.WebUtils;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import java.util.*;

/**
 * @author majiabao on 2017/12/9.
 */
public class RequestParamNotEmptyResolver extends AbstractNamedValueMethodArgumentResolver
        implements UriComponentsContributor {

    private static final TypeDescriptor STRING_TYPE_DESCRIPTOR = TypeDescriptor.valueOf(String.class);

    private final boolean useDefaultResolution;

    /**
     * @param beanFactory          a bean factory used for resolving  ${...} placeholder
     *                             and #{...} SpEL expressions in default values, or {@code null} if default
     *                             values are not expected to contain expressions
     * @param useDefaultResolution in default resolution mode a method argument
     *                             that is a simple type, as defined in {@link BeanUtils#isSimpleProperty},
     *                             is treated as a request parameter even if it itsn't annotated, the
     *                             request parameter name is derived from the method parameter name.
     */
    public RequestParamNotEmptyResolver(ConfigurableBeanFactory beanFactory,
                                        boolean useDefaultResolution) {

        super(beanFactory);
        this.useDefaultResolution = useDefaultResolution;
    }

    /**
     * Supports the following:
     * <ul>
     * <li>@RequestParamNotEmpty-annotated method arguments.
     * This excludes {@link Map} params where the annotation doesn't
     * specify a name.	See {@link requestParamMapMethodArgumentResolver}
     * instead for such params.
     * <li>Arguments of type {@link MultipartFile}
     * unless annotated with @{@link RequestPart}.
     * <li>Arguments of type {@code javax.servlet.http.Part}
     * unless annotated with @{@link RequestPart}.
     * <li>In default resolution mode, simple type arguments
     * even if not with @{@link RequestParamNotEmpty}.
     * </ul>
     */
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        Class<?> paramType = parameter.getParameterType();
        if (parameter.hasParameterAnnotation(RequestParamNotEmpty.class)) {
            if (Map.class.isAssignableFrom(paramType)) {
                String paramName = parameter.getParameterAnnotation(RequestParamNotEmpty.class).value();
                return StringUtils.hasText(paramName);
            } else {
                return true;
            }
        } else {
            if (parameter.hasParameterAnnotation(RequestPart.class)) {
                return false;
            } else if (MultipartFile.class.equals(paramType) || "javax.servlet.http.Part".equals(paramType.getName())) {
                return true;
            } else if (this.useDefaultResolution) {
                return BeanUtils.isSimpleProperty(paramType);
            } else {
                return false;
            }
        }
    }

    @Override
    protected NamedValueInfo createNamedValueInfo(MethodParameter parameter) {
        RequestParamNotEmpty annotation = parameter.getParameterAnnotation(RequestParamNotEmpty.class);
        return (annotation != null) ?
                new RequestParamNotEmptyNamedValueInfo(annotation) :
                new RequestParamNotEmptyNamedValueInfo();
    }

    @Override
    protected Object resolveName(String name, MethodParameter parameter, NativeWebRequest webRequest) throws Exception {

        Object arg;

        HttpServletRequest servletRequest = webRequest.getNativeRequest(HttpServletRequest.class);
        MultipartHttpServletRequest multipartRequest =
                WebUtils.getNativeRequest(servletRequest, MultipartHttpServletRequest.class);

        if (MultipartFile.class.equals(parameter.getParameterType())) {
            assertIsMultipartRequest(servletRequest);
            Assert.notNull(multipartRequest, "Expected MultipartHttpServletRequest: is a MultipartResolver configured?");
            arg = multipartRequest.getFile(name);
        } else if (isMultipartFileCollection(parameter)) {
            assertIsMultipartRequest(servletRequest);
            Assert.notNull(multipartRequest, "Expected MultipartHttpServletRequest: is a MultipartResolver configured?");
            arg = multipartRequest.getFiles(name);
        } else if ("javax.servlet.http.Part".equals(parameter.getParameterType().getName())) {
            assertIsMultipartRequest(servletRequest);
            arg = servletRequest.getPart(name);
        } else if (isPartCollection(parameter)) {
            assertIsMultipartRequest(servletRequest);
            arg = new ArrayList<Object>(servletRequest.getParts());
        } else {
            arg = null;
            if (multipartRequest != null) {
                List<MultipartFile> files = multipartRequest.getFiles(name);
                if (!files.isEmpty()) {
                    arg = (files.size() == 1 ? files.get(0) : files);
                }
            }
            if (arg == null) {
                String[] paramValues = webRequest.getParameterValues(name);
                if (paramValues != null) {
                    arg = paramValues.length == 1 ? paramValues[0] : paramValues;
                }
            }
        }
        RequestParamNotEmpty argumentNotEmptyAn = parameter.getParameterAnnotation(RequestParamNotEmpty.class);
        if (Objects.nonNull(argumentNotEmptyAn)) {
            if (arg instanceof String) {
                Preconditions.checkArgument(!Strings.isNullOrEmpty((String) arg), name + argumentNotEmptyAn.tip());
            } else {
                Preconditions.checkArgument(Objects.nonNull(arg), name + argumentNotEmptyAn.tip());
            }
        }
        return arg;
    }

    private void assertIsMultipartRequest(HttpServletRequest request) {
        String contentType = request.getContentType();
        if (contentType == null || !contentType.toLowerCase().startsWith("multipart/")) {
            throw new MultipartException("The current request is not a multipart request");
        }
    }

    private boolean isMultipartFileCollection(MethodParameter parameter) {
        Class<?> collectionType = getCollectionParameterType(parameter);
        return ((collectionType != null) && collectionType.equals(MultipartFile.class));
    }

    private boolean isPartCollection(MethodParameter parameter) {
        Class<?> collectionType = getCollectionParameterType(parameter);
        return ((collectionType != null) && "javax.servlet.http.Part".equals(collectionType.getName()));
    }

    private Class<?> getCollectionParameterType(MethodParameter parameter) {
        Class<?> paramType = parameter.getParameterType();
        if (Collection.class.equals(paramType) || List.class.isAssignableFrom(paramType)) {
            Class<?> valueType = GenericCollectionTypeResolver.getCollectionParameterType(parameter);
            if (valueType != null) {
                return valueType;
            }
        }
        return null;
    }

    @Override
    protected void handleMissingValue(String paramName, MethodParameter parameter) throws ServletException {
        throw new MissingServletRequestParameterException(paramName, parameter.getParameterType().getSimpleName());
    }

    @Override
    public void contributeMethodArgument(MethodParameter parameter, Object value,
                                         UriComponentsBuilder builder, Map<String, Object> uriVariables, ConversionService conversionService) {

        Class<?> paramType = parameter.getParameterType();
        if (Map.class.isAssignableFrom(paramType) || MultipartFile.class.equals(paramType) ||
                "javax.servlet.http.Part".equals(paramType.getName())) {
            return;
        }

        RequestParamNotEmpty annot = parameter.getParameterAnnotation(RequestParamNotEmpty.class);
        String name = StringUtils.isEmpty(annot.value()) ? parameter.getParameterName() : annot.value();

        if (value == null) {
            builder.queryParam(name);
        } else if (value instanceof Collection) {
            for (Object v : (Collection<?>) value) {
                v = formatUriValue(conversionService, TypeDescriptor.nested(parameter, 1), v);
                builder.queryParam(name, v);
            }
        } else {
            builder.queryParam(name, formatUriValue(conversionService, new TypeDescriptor(parameter), value));
        }
    }

    protected String formatUriValue(ConversionService cs, TypeDescriptor sourceType, Object value) {
        return (cs != null) ?
                (String) cs.convert(value, sourceType, STRING_TYPE_DESCRIPTOR) : null;
    }


    private class RequestParamNotEmptyNamedValueInfo extends NamedValueInfo {

        private RequestParamNotEmptyNamedValueInfo() {
            super("", false, ValueConstants.DEFAULT_NONE);
        }

        private RequestParamNotEmptyNamedValueInfo(RequestParamNotEmpty annotation) {
            super(annotation.value(), true, ValueConstants.DEFAULT_NONE);
        }
    }

}

