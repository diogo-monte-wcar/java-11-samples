package com.wirelesscar.dom.samples;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Http2Example {

  private static final HttpClient CLIENT = HttpClient.newBuilder().build();
  private static final List<URI> URLS = Stream.of(
      "https://en.wikipedia.org/wiki/List_of_compositions_by_Franz_Schubert",
      "https://en.wikipedia.org/wiki/2018_in_American_television",
      "https://en.wikipedia.org/wiki/List_of_compositions_by_Johann_Sebastian_Bach",
      "https://en.wikipedia.org/wiki/List_of_Australian_treaties",
      "https://en.wikipedia.org/wiki/2016%E2%80%9317_Coupe_de_France_Preliminary_Rounds",
      "https://en.wikipedia.org/wiki/Timeline_of_the_war_in_Donbass_(April%E2%80%93June_2018)",
      "https://en.wikipedia.org/wiki/List_of_giant_squid_specimens_and_sightings",
      "https://en.wikipedia.org/wiki/List_of_members_of_the_Lok_Sabha_(1952%E2%80%93present)",
      "https://en.wikipedia.org/wiki/1919_New_Year_Honours",
      "https://en.wikipedia.org/wiki/List_of_International_Organization_for_Standardization_standards"
  ).map(URI::create).collect(Collectors.toList());

  public static void main(String[] args) {
    reactiveSearch(CLIENT, URLS);
  }
  private static void reactiveSearch(HttpClient client, List<URI> urls) {
    System.out.println("-> Reactive search");
    CompletableFuture[] futures = urls.stream()
        .map(url -> reactiveSearch(client, url))
        .toArray(CompletableFuture[]::new);
    CompletableFuture.allOf(futures).join();
  }

 private static CompletableFuture<Void> reactiveSearch(HttpClient client, URI url) {
   HttpRequest httpRequest = HttpRequest.newBuilder()
       .GET().uri(url)
       .build();

   MyRequest request = new MyRequest();
   client.sendAsync(httpRequest, HttpResponse.BodyHandlers.fromLineSubscriber(request))
       .exceptionally(ex -> {
         request.onError(ex);
         return null;
       });

   return request.get()
       .exceptionally(Http2Example::handleError)
       .thenAccept(r -> handleResult(url));
 }

  private static void handleResult(URI url) {
    System.out.println("Handle result from url: " + url);
  }

  private static Void handleError(Throwable throwable) {
    System.out.println("Error");
    return null;
  }

  private static class MyRequest implements Flow.Subscriber<String> {

   private final CompletableFuture<Void> completableFuture;
   private Flow.Subscription subscription;

    private MyRequest() {
      this.completableFuture = new CompletableFuture<>();
    }

   @Override
   public void onSubscribe(Flow.Subscription subscription) {
      this.subscription = subscription;
      this.subscription.request(1);
   }

   @Override
   public void onNext(String item) {
      completableFuture.complete(null);
      subscription.request(1);
   }

   @Override
   public void onError(Throwable throwable) {
    completableFuture.completeExceptionally(throwable);
   }

   @Override
   public void onComplete() {
    completableFuture.complete(null);
   }

   public CompletableFuture<Void> get() {
    return this.completableFuture;
   }
 }
}
