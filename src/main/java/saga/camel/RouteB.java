package saga.camel;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.google.pubsub.GooglePubsubConstants;
import org.apache.camel.component.jackson.JacksonDataFormat;
import org.apache.camel.component.saga.SagaConstants;
import org.apache.camel.model.SagaCompletionMode;
import org.apache.camel.model.SagaPropagation;
import saga.services.InputA;
import saga.services.InputB;
import saga.services.ServiceA;
import saga.services.ServiceB;

import java.util.Map;

@ApplicationScoped
public class RouteB extends RouteBuilder {

    @Inject
    private ObjectMapper mapper;

    @Override
    public void configure() throws Exception {

        // pubsubの開始。
        // saga transactionを復元するため、PubSub Attributeから LRA 値を取得して所定のヘッダに設定してから Saga を開始する
        from("google-pubsub:{{app.pubsub_project}}:service-b-subscription")
                .routeId("service-b-continue-saga")
                .setHeader(SagaConstants.SAGA_LONG_RUNNING_ACTION,
                        simple("${headers." + GooglePubsubConstants.ATTRIBUTES +"[" + SagaConstants.SAGA_LONG_RUNNING_ACTION +"]}"))
                .to("direct:service-b");
        //@formatter:off
        from("direct:service-b")
                .log("saga start with ${headers} | ${body}")
                .unmarshal(new JacksonDataFormat(InputB.class))
                .saga()// Saga transactionの設定
                    // 開始済みのSaga transactionに合流することを必須にする
                    .propagation(SagaPropagation.MANDATORY)
                    .completionMode(SagaCompletionMode.MANUAL)
                    .compensation("direct:cancel-b")
                    // LRA Transactionの値を控える
                    .setVariable("transaction_context", header(SagaConstants.SAGA_LONG_RUNNING_ACTION))
                    .doTry()
                        .bean(ServiceB.class, "perform(${variable.transaction_context},${body})")
                        // 処理が終わったら、Sagaトランザクションを完了させる
                        .to("saga:complete")
                        .log("saga transaction complete success")
                    .endDoTry()
                    .doCatch(Exception.class)
                        // ルート内のエラーで、トランザクション失敗通知
                        .log("error on serviceB ${exception}")
                        .to("saga:compensate")
                    .end()
                .end();
        //@formatter:on

        // RLA Coordinator の補償処理から実行されるキャンセル処理
        from("direct:cancel-b")
                .routeId("service-b-cancel")
                .log("cancel service b with body=${body}, headers=${headers}")
                .bean(ServiceB.class, "cancel(${header." + SagaConstants.SAGA_LONG_RUNNING_ACTION +"})");
    }
}
