package net.sourceforge.stripes.springboot.autoconfigure;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.DispatcherType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.web.DispatcherServletAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import net.sourceforge.stripes.action.ActionBean;
import net.sourceforge.stripes.controller.DynamicMappingFilter;
import net.sourceforge.stripes.controller.StripesFilter;
import net.sourceforge.stripes.util.Log;
import net.sourceforge.stripes.springboot.autoconfigure.StripesProperties;

@Configuration
@ConditionalOnClass( name="net.sourceforge.stripes.springboot.autoconfigure.SpringBootVFS" ) // @see http://stackoverflow.com/a/25790672
@ConditionalOnProperty( name = "stripes.enabled", matchIfMissing = true )
@EnableConfigurationProperties( StripesProperties.class )
public class StripesAutoConfiguration {

    private static final Log LOG = Log.getInstance( StripesAutoConfiguration.class );

    private static final String BASE_PKG = ""; // would make sense to read another stripes.something property? whole classpath may be scanned twice

    @Autowired
    private StripesProperties properties;

    @Bean( name = DispatcherServletAutoConfiguration.DEFAULT_DISPATCHER_SERVLET_REGISTRATION_BEAN_NAME )
    public String disableSpringMvcDispatcherServletRegistration() {
        return "disableSpringMvcDispatcherServletRegistration";
    }

    @Bean( name = DispatcherServletAutoConfiguration.DEFAULT_DISPATCHER_SERVLET_BEAN_NAME )
    public String disableSpringMvcDispatcherServlet() {
        return "disableSpringMvcDispatcherServlet";
    }

    @Bean( name = "conventionErrorViewResolver" )
    public String disableConventionErrorViewResolver() {
        return "disableConventionErrorViewResolver";
    }

    /**
     * @return
     */
    @Bean( name = "stripesFilter" )
    @ConditionalOnMissingBean( name = "stripesFilter" )
    public FilterRegistrationBean stripesFilter() {
        final StripesFilter filter = new StripesFilter();

        final Map< String, String > params = new HashMap< String, String >();
        defaultIfEmpty( params, "ActionResolver.Packages", properties.getActionResolverPackages(), getActionResolverPackages() );
        putIfNotEmpty( params, "ActionBeanPropertyBinder.Class", properties.getActionBeanPropertyBinder() );
        putIfNotEmpty( params, "ActionBeanContext.Class", properties.getActionBeanContext() );
        putIfNotEmpty( params, "ActionBeanContextFactory.Class", properties.getActionBeanContextFactory() );
        putIfNotEmpty( params, "ActionResolver.Class", properties.getActionResolver() );
        putIfNotEmpty( params, "Configuration.Class", properties.getConfiguration() );
        putIfNotEmpty( params, "CoreInterceptor.Classes", properties.getCoreInterceptorClasses() );
        putIfNotEmpty( params, "DelegatingExceptionHandler.Packages", properties.getDelegatingExceptionHandlerPackages() );
        putIfNotEmpty( params, "ExceptionHandler.Class", properties.getExceptionHandler() );
        defaultIfEmpty( params, "Extension.Packages", properties.getExtensionPackages(), "net.sourceforge.stripes.integration.spring" );
        putIfNotEmpty( params, "FormatterFactory.Class", properties.getFormatterFactory() );
        putIfNotEmpty( params, "Interceptor.Classes", properties.getInterceptors() );
        putIfNotEmpty( params, "LocalePicker.Class", properties.getLocalePicker() );
        putIfNotEmpty( params, "LocalePicker.Locales", properties.getLocales() );
        putIfNotEmpty( params, "LocalizationBundleFactory.Class", properties.getLocalizationBundleFactory() );
        putIfNotEmpty( params, "LocalizationBundleFactory.ErrorMessageBundle", properties.getErrorMessageBundle() );
        putIfNotEmpty( params, "LocalizationBundleFactory.FieldNameBundle", properties.getFieldNameBundle() );
        putIfNotEmpty( params, "MultipartWrapper.Class", properties.getMultipartWrapper() );
        putIfNotEmpty( params, "MultipartWrapperFactory.Class", properties.getMultipartWrapperFactory() );
        putIfNotEmpty( params, "FileUpload.MaximumPostSize", properties.getFileUploadMaximumPostSize() );
        putIfNotEmpty( params, "PopulationStrategy.Class", properties.getPopulationStrategy() );
        putIfNotEmpty( params, "TagErrorRendererFactory.Class", properties.getTagErrorRendererFactory() );
        putIfNotEmpty( params, "TypeConverterFactory.Class", properties.getTypeConverterFactory() );
        putIfNotEmpty( params, "Stripes.DebugMode", properties.getDebugMode() );
        putIfNotEmpty( params, "Stripes.EncryptionKey", properties.getEncryptionKey() );
        putIfNotEmpty( params, "Stripes.HtmlMode", properties.getHtmlMode() );
        defaultIfEmpty( params, "VFS.Classes", properties.getVfsClasses(), "net.sourceforge.stripes.springboot.autoconfigure.SpringBootVFS" );
        for( final Entry< String, String > customConf : properties.getCustomConf().entrySet() ) {
            putIfNotEmpty( params, customConf.getKey(), customConf.getValue() );
        }

        final List< String > urlPatterns = new ArrayList< String >();
        urlPatterns.add( "*.jsp" );

        final FilterRegistrationBean registration = new FilterRegistrationBean();
        registration.setFilter( filter );
        registration.setInitParameters( params );
        registration.setUrlPatterns( urlPatterns );
        registration.setDispatcherTypes( DispatcherType.REQUEST );
        registration.setOrder( Ordered.HIGHEST_PRECEDENCE + 2 );
        return registration;
    }

    /**
     *
     * @return
     */
    @Bean( name = "stripesDynamicFilter" )
    @ConditionalOnMissingBean( name = "stripesDynamicFilter" )
    public FilterRegistrationBean stripesDynamicFilter() {
        final DynamicMappingFilter filter = new DynamicMappingFilter();

        final List< String > urlPatterns = new ArrayList< String >();
        urlPatterns.add( "/*" );

        final FilterRegistrationBean registration = new FilterRegistrationBean();
        registration.setFilter( filter );
        registration.setUrlPatterns( urlPatterns );
        registration.setDispatcherTypes( DispatcherType.REQUEST, DispatcherType.INCLUDE, DispatcherType.FORWARD, DispatcherType.ERROR );
        registration.setOrder( Ordered.LOWEST_PRECEDENCE );
        return registration;
    }

    String getActionResolverPackages() {
        final StripesClassesScanner< ActionBean > scanner = new StripesClassesScanner< ActionBean >();
        scanner.addIncludeFilter( new AssignableTypeFilter( ActionBean.class ) );
        final Collection< Class< ? extends ActionBean > > actionbeans = scanner.findComponentClasses( BASE_PKG );
        final String packages = scanner.toPackagesWithoutStripesClasses( actionbeans );
        Assert.state( !StringUtils.isEmpty( packages ),
                      "Didn't find classes implementing ActionBean, check your application build and optionally your " +
                      "stripes.action-resolver-packages property on application.properties" );

        LOG.info( "Detected ActionBeans on " + packages );

        return packages;
    }

    void defaultIfEmpty( final Map< String, String > params, final String key, final String value, final String def ) {
        if( StringUtils.isEmpty( value ) ) {
            putIfNotEmpty( params, key, def );
        } else {
            putIfNotEmpty( params, key, value );
        }
    }

    void putIfNotEmpty( final Map< String, String > params, final String key, final String value ) {
        if( !StringUtils.isEmpty( key ) && !StringUtils.isEmpty( value ) ) {
            LOG.debug( "Stripes configuration: param " + key + " with value " + value );
            params.put( key, value );
        }
    }

}
