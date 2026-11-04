package com.santacecilia.api.service;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.*;
import java.net.URI;
@Component
public class RecoveryMailSender {
    private final org.springframework.core.task.TaskExecutor executor;
    private final ObjectProvider<JavaMailSender> mail;private final String host,from,base;
    private static final org.slf4j.Logger LOG=org.slf4j.LoggerFactory.getLogger(RecoveryMailSender.class);
    public RecoveryMailSender(@org.springframework.beans.factory.annotation.Qualifier("correoExecutor") org.springframework.core.task.TaskExecutor executor,ObjectProvider<JavaMailSender> mail,@Value("${spring.mail.host:}") String host,
        @Value("${app.mail-from:}") String from,@Value("${app.public-url:}") String base){
        this.executor=executor;this.mail=mail;this.host=host;this.from=from;this.base=base.replaceAll("/+$","");
    }
    public boolean disponible(){
        if(host.isBlank()||from.isBlank()||base.isBlank()||mail.getIfAvailable()==null)return false;
        try{URI url=URI.create(base);return "https".equals(url.getScheme())||("http".equals(url.getScheme())&&("127.0.0.1".equals(url.getHost())||"localhost".equals(url.getHost())));}
        catch(IllegalArgumentException e){return false;}
    }
    @TransactionalEventListener(phase=TransactionPhase.AFTER_COMMIT)
    public void enviar(RecoveryService.Solicitada event){
        try{executor.execute(()->entregar(event));}
        catch(org.springframework.core.task.TaskRejectedException error){LOG.error("Cola de correo temporalmente saturada");}
    }
    private void entregar(RecoveryService.Solicitada event){
        try{
            SimpleMailMessage message=new SimpleMailMessage();message.setFrom(from);message.setTo(event.email());
            message.setSubject("Santa Cecilia - Recuperacion de acceso");
            message.setText("Solicitaste restablecer tu acceso a Santa Cecilia. Abre este enlace de un solo uso:\n"+base+"/restablecer.html#token="+event.token()+"\nSi no lo solicitaste, ignora este mensaje.");
            mail.getObject().send(message);
        }catch(Exception error){LOG.error("No se pudo entregar un correo de recuperacion; tipo={}",error.getClass().getSimpleName());}
    }
}
