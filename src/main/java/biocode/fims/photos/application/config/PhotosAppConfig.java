package biocode.fims.photos.application.config;

import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

/**
 * Configuration class for biocode-fims-photos application.
 */
@Configuration
public class PhotosAppConfig {
    @Bean
    public PhotosSql photosSql() {
        YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
        yaml.setResources(new ClassPathResource("biocode-fims-photos-sql.yml"));
        return new PhotosSql(yaml.getObject());
    }
}
