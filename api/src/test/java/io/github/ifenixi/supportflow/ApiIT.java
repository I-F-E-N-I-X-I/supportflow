package io.github.ifenixi.supportflow;
import java.util.*;
import java.util.concurrent.*;
import java.time.Duration;
import com.fasterxml.jackson.databind.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.testcontainers.containers.*;
import org.testcontainers.junit.jupiter.*;
import org.testcontainers.junit.jupiter.Container;
import io.github.ifenixi.supportflow.identity.*;
import io.github.ifenixi.supportflow.event.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest @AutoConfigureMockMvc @Testcontainers
class ApiIT {
 @Container static PostgreSQLContainer<?> pg=new PostgreSQLContainer<>("postgres:17-alpine");
 @Container static RabbitMQContainer rabbit=new RabbitMQContainer("rabbitmq:4.1-alpine");
 @Container static GenericContainer<?> redis=new GenericContainer<>("redis:7.4-alpine").withExposedPorts(6379);
 @DynamicPropertySource static void properties(DynamicPropertyRegistry r){
  r.add("spring.datasource.url",pg::getJdbcUrl);r.add("spring.datasource.username",pg::getUsername);r.add("spring.datasource.password",pg::getPassword);
  r.add("spring.rabbitmq.host",rabbit::getHost);r.add("spring.rabbitmq.port",rabbit::getAmqpPort);r.add("spring.rabbitmq.username",rabbit::getAdminUsername);r.add("spring.rabbitmq.password",rabbit::getAdminPassword);
  r.add("spring.data.redis.host",redis::getHost);r.add("spring.data.redis.port",()->redis.getMappedPort(6379));r.add("spring.data.redis.password",()->"");
  r.add("app.jwt-secret",()->"test-only-jwt-secret-at-least-32-characters");r.add("app.admin-password",()->"test-admin-password");r.add("app.internal-token",()->"test-only-internal-token-at-least-32-chars");r.add("app.outbox-enabled",()->false);
 }
 @Autowired MockMvc mvc;@Autowired ObjectMapper json;@Autowired AccountRepository users;@Autowired PasswordEncoder passwords;@Autowired JdbcTemplate jdbc;
 @Autowired OutboxPublisher outbox;@Autowired RabbitTemplate broker;@Autowired StringRedisTemplate cache;
 long client,other,support,admin;
 @BeforeEach void users(){client=account(Role.CLIENT);other=account(Role.CLIENT);support=account(Role.SUPPORT);admin=users.findByUsername("admin").orElseThrow().getId();}
 long account(Role role){return users.saveAndFlush(new Account("u"+UUID.randomUUID().toString().replace("-",""),passwords.encode("test-user-password"),role)).getId();}
 MockHttpServletRequestBuilder as(MockHttpServletRequestBuilder request,long id){return request.with(jwt().jwt(j->j.subject(Long.toString(id))));}
 JsonNode send(MockHttpServletRequestBuilder request,int expected) throws Exception {var result=mvc.perform(request).andExpect(status().is(expected)).andReturn();String body=result.getResponse().getContentAsString();return body.isBlank()?json.nullNode():json.readTree(body);}
 MockHttpServletRequestBuilder body(MockHttpServletRequestBuilder request,Object value) throws Exception {return request.contentType("application/json").content(json.writeValueAsBytes(value));}
 Map<String,Object> draft(){return Map.of("title","Payment question","description","Please check the invoice","priority","HIGH","tagIds",List.of(1));}
 JsonNode create() throws Exception {return create(UUID.randomUUID().toString(),client);}
 JsonNode create(String key,long owner) throws Exception {return send(body(as(post("/api/v1/tickets"),owner),draft()).header("Idempotency-Key",key),201);}
 long assigned() throws Exception {long id=create().get("id").asLong();send(body(as(patch("/api/v1/tickets/"+id+"/assignee"),admin),Map.of("assigneeId",support)),200);return id;}
 void transition(long id,String value,long actor,int code) throws Exception {send(body(as(patch("/api/v1/tickets/"+id+"/status"),actor),Map.of("status",value)),code);}
 @Test void publicSwaggerWorks() throws Exception {mvc.perform(get("/v3/api-docs")).andExpect(status().isOk()).andExpect(jsonPath("openapi").exists());}
 @Test void anonymousIsRejected() throws Exception {mvc.perform(get("/api/v1/tickets")).andExpect(status().isUnauthorized());}
 @Test void forgedTokenIsRejected() throws Exception {mvc.perform(get("/api/v1/tickets").header("Authorization","Bearer not-a-token")).andExpect(status().isUnauthorized());}
 @Test void registerLoginAndUseRealJwt() throws Exception {
  String username="auth"+UUID.randomUUID().toString().replace("-","");var credentials=Map.of("username",username,"password","test-user-password");
  var user=send(body(post("/api/v1/auth/register"),credentials),201);assertEquals("CLIENT",user.get("role").asText());assertFalse(user.has("passwordHash"));
  var token=send(body(post("/api/v1/auth/login"),credentials),200).get("accessToken").asText();
  mvc.perform(get("/api/v1/auth/me").header("Authorization","Bearer "+token)).andExpect(status().isOk()).andExpect(jsonPath("username").value(username));
 }
 @Test void duplicateUsernameIsConflict() throws Exception {send(body(post("/api/v1/auth/register"),Map.of("username","admin","password","test-user-password")),409);}
 @Test void unknownLoginRejected() throws Exception {send(body(post("/api/v1/auth/login"),Map.of("username","unknown","password","test-user-password")),401);}
 @Test void roleInjectionRejected() throws Exception {send(body(post("/api/v1/auth/register"),Map.of("username","intruder","password","test-user-password","role","ADMIN")),400);}
 @Test void shortPasswordRejected() throws Exception {send(body(post("/api/v1/auth/register"),Map.of("username","intruder","password","short")),400);}
 @Test void clientCannotPromoteSelf() throws Exception {send(body(as(patch("/api/v1/admin/users/"+client+"/role"),client),Map.of("role","SUPPORT")),403);}
 @Test void adminCanPromoteSupport() throws Exception {send(body(as(patch("/api/v1/admin/users/"+client+"/role"),admin),Map.of("role","SUPPORT")),200);assertEquals(Role.SUPPORT,users.findById(client).orElseThrow().getRole());}
 @Test void adminCannotBeDemoted() throws Exception {send(body(as(patch("/api/v1/admin/users/"+admin+"/role"),admin),Map.of("role","CLIENT")),409);}
 @Test void createsTicketHistoryAndOutboxAtomically() throws Exception {long id=create().get("id").asLong();assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM status_history WHERE ticket_id=?",Integer.class,id));assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM outbox_event WHERE ticket_id=?",Integer.class,id));}
 @Test void invalidTagDoesNotCreateTicketOrEvent() throws Exception {var d=new HashMap<>(draft());d.put("tagIds",List.of(999999));send(body(as(post("/api/v1/tickets"),client),d).header("Idempotency-Key",UUID.randomUUID().toString()),404);assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM ticket WHERE client_id=?",Integer.class,client));}
 @Test void missingIdempotencyKeyRejected() throws Exception {send(body(as(post("/api/v1/tickets"),client),draft()),400);}
 @Test void repeatedRequestReturnsSameTicket() throws Exception {String key=UUID.randomUUID().toString();assertEquals(create(key,client).get("id"),create(key,client).get("id"));}
 @Test void changedRequestWithSameKeyIsConflict() throws Exception {String key=UUID.randomUUID().toString();create(key,client);var d=new HashMap<>(draft());d.put("title","Different");send(body(as(post("/api/v1/tickets"),client),d).header("Idempotency-Key",key),409);}
 @Test void concurrentDuplicateCreateOnlyCreatesOneTicket() throws Exception {
  String key=UUID.randomUUID().toString();var start=new CountDownLatch(1);
  try(var executor=Executors.newFixedThreadPool(4)) {List<Future<Long>> results=new ArrayList<>();for(int i=0;i<4;i++)results.add(executor.submit(()->{start.await();return create(key,client).get("id").asLong();}));start.countDown();Set<Long> ids=new HashSet<>();for(var f:results)ids.add(f.get(20,TimeUnit.SECONDS));assertEquals(1,ids.size());}
  assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM ticket WHERE client_id=?",Integer.class,client));
 }
 @Test void otherClientCannotReadTicketHistoryOrComments() throws Exception {long id=create().get("id").asLong();for(String suffix:List.of("","/history","/comments","/tags"))send(as(get("/api/v1/tickets/"+id+suffix),other),404);}
 @Test void clientListIsIsolated() throws Exception {create();assertEquals(0,send(as(get("/api/v1/tickets"),other),200).get("total").asLong());}
 @Test void queryFilterAndPagination() throws Exception {create();assertEquals(1,send(as(get("/api/v1/tickets?query=payment&priority=HIGH&status=NEW&size=1"),client),200).get("items").size());assertEquals(0,send(as(get("/api/v1/tickets?priority=LOW"),client),200).get("total").asLong());}
 @Test void invalidPaginationAndSort() throws Exception {for(String query:List.of("size=101","page=-1","sort=passwordHash","status=UNKNOWN"))send(as(get("/api/v1/tickets?"+query),client),400);}
 @Test void sqlWildcardIsLiteral() throws Exception {create();assertEquals(0,send(as(get("/api/v1/tickets").param("query","%"),client),200).get("total").asLong());}
 @Test void lifecycleAndClientClose() throws Exception {long id=assigned();transition(id,"IN_PROGRESS",support,200);transition(id,"RESOLVED",support,200);transition(id,"CLOSED",client,200);assertEquals(4,send(as(get("/api/v1/tickets/"+id+"/history"),client),200).size());}
 @Test void cannotStartWithoutAssignee() throws Exception {transition(create().get("id").asLong(),"IN_PROGRESS",admin,409);}
 @Test void clientCannotResolve() throws Exception {transition(assigned(),"RESOLVED",client,403);}
 @Test void cannotSkipStatuses() throws Exception {transition(assigned(),"CLOSED",support,409);}
 @Test void resolvedCanBeReopened() throws Exception {long id=assigned();transition(id,"IN_PROGRESS",support,200);transition(id,"RESOLVED",support,200);transition(id,"IN_PROGRESS",support,200);assertTrue(send(as(get("/api/v1/tickets/"+id),client),200).get("resolvedAt").isNull());}
 @Test void staleVersionIsRejected() throws Exception {var t=create();long id=t.get("id").asLong();var revision=new HashMap<>(draft());revision.put("version",t.get("version").asLong());revision.put("title","Updated");send(body(as(put("/api/v1/tickets/"+id),client),revision),200);send(body(as(put("/api/v1/tickets/"+id),client),revision),409);}
 @Test void clientCannotAssign() throws Exception {long id=create().get("id").asLong();send(body(as(patch("/api/v1/tickets/"+id+"/assignee"),client),Map.of("assigneeId",support)),403);}
 @Test void cannotAssignClient() throws Exception {long id=create().get("id").asLong();send(body(as(patch("/api/v1/tickets/"+id+"/assignee"),admin),Map.of("assigneeId",client)),409);}
 @Test void internalCommentHiddenFromClient() throws Exception {long id=assigned();send(body(as(post("/api/v1/tickets/"+id+"/comments"),support),Map.of("body","Staff note","internal",true)),201);assertEquals(0,send(as(get("/api/v1/tickets/"+id+"/comments"),client),200).get("total").asInt());assertEquals(1,send(as(get("/api/v1/tickets/"+id+"/comments"),support),200).get("total").asInt());}
 @Test void clientCannotWriteInternalComment() throws Exception {long id=create().get("id").asLong();send(body(as(post("/api/v1/tickets/"+id+"/comments"),client),Map.of("body","Hidden","internal",true)),403);}
 @Test void publicCommentVisible() throws Exception {long id=create().get("id").asLong();send(body(as(post("/api/v1/tickets/"+id+"/comments"),client),Map.of("body","Details","internal",false)),201);assertEquals(1,send(as(get("/api/v1/tickets/"+id+"/comments"),client),200).get("total").asInt());}
 @Test void compensationIsExactDecimal() throws Exception {long id=create().get("id").asLong();var t=send(body(as(patch("/api/v1/tickets/"+id+"/compensation"),admin),Map.of("amount","123.45")),200);assertEquals("123.45",t.get("compensation").asText());assertEquals(new java.math.BigDecimal("123.45"),jdbc.queryForObject("SELECT compensation FROM ticket WHERE id=?",java.math.BigDecimal.class,id));}
 @Test void negativeCompensationRejected() throws Exception {long id=create().get("id").asLong();send(body(as(patch("/api/v1/tickets/"+id+"/compensation"),admin),Map.of("amount","-0.01")),400);}
 @Test void clientsCannotReadStaffReports() throws Exception {send(as(get("/api/v1/reports/summary"),client),403);send(as(get("/api/v1/reports/workload"),client),403);}
 @Test void reportsIncludeUnassignedAndZeroWorkload() throws Exception {create();assertTrue(send(as(get("/api/v1/reports/summary"),support),200).size()>0);assertTrue(send(as(get("/api/v1/reports/workload?minimum=0"),support),200).size()>0);}
 @Test void cachePopulatesAndInvalidatesAfterTagCreation() throws Exception {cache.delete("supportflow:tags:v1");send(as(get("/api/v1/tags"),client),200);assertTrue(Boolean.TRUE.equals(cache.hasKey("supportflow:tags:v1")));send(body(as(post("/api/v1/tags"),admin),Map.of("name","tag"+UUID.randomUUID().toString().replace("-",""))),201);assertFalse(Boolean.TRUE.equals(cache.hasKey("supportflow:tags:v1")));}
 @Test void unusedTagCanBeDeleted() throws Exception {long id=send(body(as(post("/api/v1/tags"),admin),Map.of("name","tag"+UUID.randomUUID().toString().replace("-",""))),201).get("id").asLong();send(as(delete("/api/v1/tags/"+id),admin),204);send(as(delete("/api/v1/tags/"+id),admin),404);}
 @Test void referencedTagCannotBeDeleted() throws Exception {create();send(as(delete("/api/v1/tags/1"),admin),409);}
 @Test void publisherConfirmsBeforeMarkingPublished() throws Exception {long id=create().get("id").asLong();for(int i=0;i<30;i++){outbox.publishBatch();if(jdbc.queryForObject("SELECT COUNT(*) FROM outbox_event WHERE ticket_id=? AND published_at IS NULL",Integer.class,id)==0)break;}assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM outbox_event WHERE ticket_id=? AND published_at IS NULL",Integer.class,id));assertNotNull(broker.receive(BrokerConfig.QUEUE,3000));}
}
