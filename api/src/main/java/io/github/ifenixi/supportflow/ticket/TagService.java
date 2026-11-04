package io.github.ifenixi.supportflow.ticket;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.*;
import io.github.ifenixi.supportflow.identity.Actor;
import io.github.ifenixi.supportflow.common.DomainException;
@Service
public class TagService {
 private final TagRepository tags;private final Actor actor;private final TagCache cache;
 public TagService(TagRepository tags,Actor actor,TagCache cache){this.tags=tags;this.actor=actor;this.cache=cache;}
 public List<TicketDtos.TagView> list(){actor.current();var cached=cache.read();if(cached.isPresent())return cached.get();var values=tags.findAll(org.springframework.data.domain.Sort.by("name")).stream().map(t->new TicketDtos.TagView(t.getId(),t.getName())).toList();cache.write(values);return values;}
 @Transactional public TicketDtos.TagView create(String name){actor.admin();var tag=tags.saveAndFlush(new Tag(name.strip().toLowerCase(Locale.ROOT)));invalidateAfterCommit();return new TicketDtos.TagView(tag.getId(),tag.getName());}
 @Transactional public void delete(long id){actor.admin();var tag=tags.findById(id).orElseThrow(DomainException::missing);tags.delete(tag);tags.flush();invalidateAfterCommit();}
 private void invalidateAfterCommit(){TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization(){@Override public void afterCommit(){cache.invalidate();}});}
}
