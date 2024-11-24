package saga.camel;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.google.pubsub.GooglePubsubConstants;
import org.apache.camel.component.jackson.JacksonDataFormat;
import org.apache.camel.component.saga.SagaConstants;
import org.apache.camel.model.SagaCompletionMode;
import saga.services.InputA;
import saga.services.ServiceA;

import java.util.Map;

@ApplicationScoped
public class RouteA extends RouteBuilder {

    @Inject
    private ObjectMapper mapper;

    @Override
    public void configure() throws Exception {

        //@formatter:off
        // PubSub経由でメッセージを受け取り、Saga transactionを開始する
        from("google-pubsub:{{app.pubsub_project}}:service-a-subscription")
            .routeId("service-a-start-saga")
            .log("saga start with ${headers} | ${body}")
            .unmarshal(new JacksonDataFormat(InputA.class))
            .saga()// Saga transactionの設定
                /*
                   トランザクション制御をマニュアルに設定する。
                   https://camel.apache.org/components/4.4.x/eips/saga-eip.html#_using_manual_completion_advanced
                   AUTOの場合、このルートが終了するとtransactionが自動で完了する。
                   PubSubなど非同期通信で別のルートが起動する場合だとAUTOは使用できない。
                   一方で、https://github.com/apache/camel-spring-boot-examples/blob/main/saga/readme.adoc
                   のサンプルの場合、JMSを使っているがJMSの応答を同期処理的に待ち合わせる replyの仕組みが使えるので利用する技術が同期応答可能ならAUTOが利用できる。
                 */
                .completionMode(SagaCompletionMode.MANUAL)
                // 補償トランザクションが行われた場合のルート。どこかでエラーが起きてsaga:compensationが実行されたら、LRA Coordinator 経由で呼び出される
                .compensation("direct:cancel-a")
                // variableにLRA の値を保管しておく
                .setVariable("transaction_context", header(SagaConstants.SAGA_LONG_RUNNING_ACTION))
                .doTry()
                    // サービスA処理の実行後、後続サービスをPubSub経由で呼び出す
                    .bean(ServiceA.class, "perform(${variable.transaction_context},${body})")
                    // pubsub連携する場合は、Long-Running-Actionヘッダの値を自前で送信・復元する必要がある。
                    // 現時点でDSLでうまく設定できないため、processで記述する
                    .process(exchange -> {
                        exchange.getIn().setHeader(GooglePubsubConstants.ATTRIBUTES,
                                Map.of(SagaConstants.SAGA_LONG_RUNNING_ACTION, exchange.getVariable("transaction_context")));
                    })
                    .to("google-pubsub:{{app.pubsub_project}}:service-b-topic?exchangePattern=InOnly")
                .endDoTry()
                .doCatch(Exception.class)
                    // 処理中の例外は saga:compensate を呼び出して補償トランザクションの実行を指示する。
                    .log("error on serviceA ${exception}")
                    .to("saga:compensate")
                .end()
            .end();
        //@formatter:on
        from("direct:cancel-a")
                .routeId("service-a-cancel")
                .log("cancel service a with body=${body}, headers=${headers}")
                .bean(ServiceA.class, "cancel(${header." + SagaConstants.SAGA_LONG_RUNNING_ACTION +"})");
    }
}
