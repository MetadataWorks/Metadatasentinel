package uk.co.metadataconsulting.sentinel

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import grails.config.Config
import grails.core.support.GrailsConfigurationAware
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import uk.co.metadataconsulting.sentinel.modelcatalogue.ValidationRules

@Slf4j
@CompileStatic
class RuleFetcherService implements GrailsConfigurationAware {

    private final Moshi moshi = new Moshi.Builder().build()
    private final OkHttpClient client = new OkHttpClient()
    private final JsonAdapter<ValidationRules> validationRulesJsonAdapter = moshi.adapter(ValidationRules.class)

    String metadataUrl

    Map<String, ValidationRules> fetchValidationRules(List<String> gormUrls) {
        Map<String, ValidationRules> gormUrlsRules = [:]
        for ( String gormUrl : gormUrls ) {
            ValidationRules validationRules = fetchValidationRules(gormUrl)
            if ( validationRules ) {
                gormUrlsRules[gormUrl] = validationRules
            }
        }
        gormUrlsRules
    }

    ValidationRules fetchValidationRules(String gormUrl) {

        final String url = "${metadataUrl}/api/modelCatalogue/core/validationRule/rules?gormUrl=${gormUrl}".toString()
        HttpUrl.Builder httpBuider = HttpUrl.parse(url).newBuilder()
        Request request = new Request.Builder()
                .url(httpBuider.build())
                .header("Accept", 'application/json')
                .build()
        ValidationRules validationRules

        try {
            Response response = client.newCall(request).execute()

            if ( response.isSuccessful()  ) {
                if ( response.code() == 200 ) {
                    validationRules = validationRulesJsonAdapter.fromJson(response.body().source())
                }
            } else {
                log.warn 'Response {}. Could not fetch github repository at {}', response.code(), url
            }
            response.close()
        } catch (IOException ioexception) {
            log.warn('unable to connect to server {}', metadataUrl)
        }

        validationRules
    }

    @Override
    void setConfiguration(Config co) {
        metadataUrl = co.getProperty('metadata.url', String, 'http://localhost:8080')
    }
}
