package saga;

// https://camel.apache.org/components/4.8.x/eips/saga-eip.html

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import org.apache.camel.quarkus.core.events.ComponentAddEvent;
import org.apache.camel.service.lra.LRASagaService;

/**
 * camel context に LRA Saga serviceを設定するためのQuarkus起動設定
 * https://camel.apache.org/components/4.4.x/others/lra.html
 */
@ApplicationScoped
public class CamelSagaConfig {


    public void onComponentAdd(@Observes ComponentAddEvent event) throws Exception {
        if (event.getContext().hasService(LRASagaService.class) == null) {
            LRASagaService service = new LRASagaService();
            // LRA Coordinator(トランザクションマネージャ)のURL
            service.setCoordinatorUrl("http://localhost:8081");
            /*
              LRA Coordinator に伝える、自身のアプリケーションのURL。
              Saga トランザクション参加の際にこのURLを伝え、
              何らかのエラーで補償トランザクションを実行する場合にCoordinatorがこのURLで呼び出す。
              Coordinatorは今回はDockerで実行しているので、Dockerから解決可能なホストアドレス(host.docker.internal)にしている。
             */
            service.setLocalParticipantUrl("http://host.docker.internal:8080");
            event.getContext().addService(service);
        }
    }
}
