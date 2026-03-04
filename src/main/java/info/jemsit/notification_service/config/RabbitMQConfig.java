package info.jemsit.notification_service.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static info.jemsit.common.data.constants.RabbitMQConstants.*;


@Configuration
public class RabbitMQConfig {

    @Bean
    public Queue mediaQueue() {
        return new Queue(MEDIA_QUEUE, true);
    }

    @Bean
    public TopicExchange mediaExchange() {
        return new TopicExchange(MEDIA_EXCHANGE);
    }

    @Bean
    public Binding mediaBinding(Queue mediaQueue, TopicExchange mediaExchange) {
        return BindingBuilder.bind(mediaQueue).to(mediaExchange).with(MEDIA_KEY);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
