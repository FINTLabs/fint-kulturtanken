package no.fint;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import reactor.netty.http.client.HttpClient;
import reactor.netty.tcp.TcpClient;

@Configuration
public class ApplicationConfiguration {

    @Value("${fint.dks.baseUrl:https://beta.felleskomponent.no/}")
    private String baseUrl;

    @Value("${webclient.handler.timeout.value}")
    private int timeoutValue;

    @Bean
    public WebClient webClient() {

        TcpClient tcpClient = TcpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 1000)
                .doOnConnected(connection ->
                        connection.addHandlerLast(new ReadTimeoutHandler(timeoutValue))
                                .addHandlerLast(new WriteTimeoutHandler(timeoutValue)));
        return WebClient.builder()
                .baseUrl(baseUrl)
                .clientConnector(new ReactorClientHttpConnector(HttpClient.from(tcpClient)))
                .build();
    }
}
