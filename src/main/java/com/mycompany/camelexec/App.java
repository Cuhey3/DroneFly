package com.mycompany.camelexec;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.main.Main;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;
import org.apache.camel.component.exec.ExecResult;

// Drone fly in response to the bot
public class App {

  public static void main(String[] args) throws Exception {
    // you can generate token on https://api.slack.com/docs/oauth-test-tokens
    final String YOUR_TOKEN = "";
    Connection.Response execute = Jsoup.connect("https://slack.com/api/rtm.start?token="+YOUR_TOKEN).ignoreContentType(true).execute();
    ObjectMapper mapper = new ObjectMapper();
    Map<String, Object> jsonObject = mapper.readValue(execute.body(), Map.class);
    final Object websocket_uri = (jsonObject.get("url")).toString().replace("wss://", "");

    Main main = new Main();
    main.addRouteBuilder(new RouteBuilder() {
      @Override
      public void configure() throws Exception {
        // read websocket
        from("ahc-wss://" + websocket_uri)
                .process((Exchange exchange) -> {
                  System.out.println(exchange.getIn().getHeaders());
                  System.out.println(exchange.getIn().getBody());
                })
                .to("log:foo")
                .filter((Exchange exchange)->{
                  // write bot recognizing process
                  return false;
                })
                // execute node script
                .to("exec:/usr/local/bin/node?args=my.js&workingDir=/Users/fast-it2/my")
                // get execution result
                .process((Exchange exchange) -> {
                  ExecResult body = exchange.getIn().getBody(ExecResult.class);
                  InputStream stdout = body.getStdout();
                  BufferedReader br = new BufferedReader(new InputStreamReader(stdout));
                  br.lines().forEach(System.out::println);
                });
        // write websocket: needs for start reading websocket
        from("timer:foo?period=60s").setBody(constant("{\"type\":\"hello\"}")).to("ahc-wss://" + websocket_uri);
      }
    });
    main.run();
  }
}
