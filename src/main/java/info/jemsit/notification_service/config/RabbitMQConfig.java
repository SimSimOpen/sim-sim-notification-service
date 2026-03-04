package info.jemsit.notification_service.config;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static info.jemsit.common.data.constants.RabbitMQConstants.MEDIA_QUEUE;


@Configuration
public class RabbitMQConfig {

    @Bean
    public Queue mediaQueue() {
        return new Queue(MEDIA_QUEUE, true);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
