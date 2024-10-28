package bitc.fullstack.meausrepro_spring.controller;

import bitc.fullstack.meausrepro_spring.model.MeausreProReport;
import bitc.fullstack.meausrepro_spring.service.ReportService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/MeausrePro/report")
public class ReportController {

    private final ReportService reportService;
    private final S3Client s3Client;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    @Value("${aws.s3.base-url}")
    private String s3BaseUrl;

    public ReportController(ReportService reportService, @Value("${aws.s3.region}") String region) {
        this.reportService = reportService;
        this.s3Client = S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(ProfileCredentialsProvider.create())
                .build();
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadReport(@RequestParam("file") MultipartFile file, @RequestParam("sectionId") int sectionId, @RequestParam("userId") int userId) {
        try {
            String fileName = "reports/" + file.getOriginalFilename();

            // S3에 파일 업로드
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileName)
                    .acl("public-read")
                    .build();
            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(file.getBytes()));

            // S3 URL 생성
            String fileUrl = s3BaseUrl + "/" + fileName;

            // 파일 정보 DB 저장
            MeausreProReport report = new MeausreProReport();
            report.setFileName(file.getOriginalFilename());
            report.setFilePath(fileUrl);
            report.setUploadDate(LocalDateTime.now().toString());

            // 서비스 호출하여 보고서 저장
            reportService.saveReport(report, sectionId, userId);

            return ResponseEntity.ok("파일 업로드 성공");
        } catch (IOException | S3Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("파일 업로드 실패");
        }
    }

    // 구간별 report 리스트
    @GetMapping("/reports/{sectionId}")
    public List<MeausreProReport> getReportsBySection(@PathVariable int sectionId) {
        return reportService.getReportsBySection(sectionId);
    }

    // 리포트 삭제
    @DeleteMapping("/delete/{idx}")
    public ResponseEntity<String> deleteReport(@PathVariable int idx) {
        return reportService.deleteByReportIdx(idx);
    }
}
