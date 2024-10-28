package bitc.fullstack.meausrepro_spring.service;

import bitc.fullstack.meausrepro_spring.model.MeausreProImg;
import bitc.fullstack.meausrepro_spring.model.MeausreProSection;
import bitc.fullstack.meausrepro_spring.repository.ImgRepository;
import bitc.fullstack.meausrepro_spring.repository.SectionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ImgService {
    @Autowired
    private ImgRepository imgRepository;

    @Autowired
    private SectionRepository sectionRepository;

    private final S3Client s3Client;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    @Value("${aws.s3.base-url}")
    private String s3BaseUrl;

    public ImgService(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    // 이미지 다운시 필요
    public String getFileUrl(String fileName) {
        return s3BaseUrl + "/images/" + fileName;
    }

    // 이미지 저장
    @Transactional
    public MeausreProImg uploadImage(MultipartFile file, int sectionId) {
        try {
            String uniqueFileName = "images/" + UUID.randomUUID() + "_" + file.getOriginalFilename();

            // S3에 파일 업로드
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(uniqueFileName)
                    .acl("public-read")
                    .build();
            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(file.getBytes()));

            // S3 URL 생성
            String fileUrl = s3BaseUrl + "/" + uniqueFileName;

            // 데이터베이스에 이미지 정보 저장
            MeausreProSection section = sectionRepository.findByIdx(sectionId)
                    .orElseThrow(() -> new IllegalArgumentException("구간이 존재하지 않습니다."));

            MeausreProImg img = new MeausreProImg();
            img.setSectionId(section);
            img.setImgSrc(fileUrl);
            img.setImgDes(null);

            return imgRepository.save(img);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    // 특정 구간 이미지 보기
    public List<MeausreProImg> sectionImages(int sectionId) {
        return imgRepository.findAllBySectionId(sectionId);
    }

    // 이미지 설명 수정
    public boolean updateImgDes(MeausreProImg image) {
        Optional<MeausreProImg> existingImgDes = imgRepository.findByIdx(image.getIdx());
        if (existingImgDes.isPresent()) {
            MeausreProImg updatedImgDes = existingImgDes.get();
            updatedImgDes.setImgDes(image.getImgDes());
            imgRepository.save(updatedImgDes);
            return true;
        }
        return false;
    }

    // 이미지 삭제
    @Transactional
    public boolean deleteImage(int idx) {
        Optional<MeausreProImg> imgOptional = imgRepository.findByIdx(idx);

        if (imgOptional.isPresent()) {
            MeausreProImg img = imgOptional.get();
            deleteFileFromS3(img.getImgSrc());
            imgRepository.delete(img);
            return true;
        }
        return false;
    }

    // S3 파일 삭제
    private void deleteFileFromS3(String fileUrl) {
        String s3Key = fileUrl.replace(s3BaseUrl + "/", "");
        s3Client.deleteObject(DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .build());
    }

    public void deleteBySectionIdx(int idx) {
        List<MeausreProImg> imgList = imgRepository.findAllBySectionId(idx);

        for (MeausreProImg img : imgList) {
            deleteImage(img.getIdx());
        }
    }
}