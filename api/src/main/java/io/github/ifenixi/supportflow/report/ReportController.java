package io.github.ifenixi.supportflow.report;
import java.util.List;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/reports")
public class ReportController {
 private final ReportService service;
 public ReportController(ReportService service){this.service=service;}
 @GetMapping("/summary") public List<ReportService.StatusSummary> summary(){return service.summary();}
 @GetMapping("/workload") public List<ReportService.Workload> workload(@RequestParam(defaultValue="0") int minimum){return service.workload(minimum);}
}
