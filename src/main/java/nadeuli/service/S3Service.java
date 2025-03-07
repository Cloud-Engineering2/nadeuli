/* S3Service.java
 * nadeuli Service - 여행
 * AWS S3 관련 Service
 * 작성자 : 이홍비
 * 최종 수정 날짜 : 2025.02.25
 *
 * ========================================================
 * 프로그램 수정 / 보완 이력
 * ========================================================
 * 작업자        날짜        수정 / 보완 내용
 * ========================================================
 * 이홍비    2025.02.25     최초 작성 : S3Service
 * 이홍비    2025.02.26     s3 에서 파일 삭제 시 cloudfront cache 무효화 처리
 * 이홍비    2025.02.26     extractRelativePathFromUrl() 구현
 * 이홍비    2025.02.28     kind 를 열거형으로 변경함
 * 이홍비    2025.03.01     @Transactional 추가
 * 이홍비    2025.03.03     파일 다운로드 관련 처리 추가
 *                         불필요한 것 처리
 * 이홍비    2025.03.05     지역 사진 저장 경로 추가
 * ========================================================
 */

package nadeuli.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import nadeuli.common.PhotoType;
import nadeuli.repository.JournalRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class S3Service {

    private final AmazonS3 amazonS3;
    private final JournalRepository journalRepository;

    @Value("${cloud.aws.s3.bucket}")
    private String bucketName;

    @Value("${cloudfront.url}") // CloudFront 도메인 설정
    private String cloudFrontUrl;

    private final String JOURNAL_DIR_NAME = "journal";
    private final String PROFILE_DIR_NAME = "profile";
    private final String REGION_DIR_NAME = "region";
    private final String ETC_DIR_NAME = "etc";

    // 사진 올리기
    public String uploadFile(MultipartFile file, PhotoType kind) {
        System.out.println("🔥 S3 & Cloud Front - 사진 올리기 실행!");

        try {
            String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
            String s3Key;
            if (kind == PhotoType.PROFILE) {
                // 프로필 사진
                s3Key = PROFILE_DIR_NAME + "/" + fileName;
            }
            else if (kind == PhotoType.JOURNAL) {
                // 여행 사진
                s3Key = JOURNAL_DIR_NAME + "/" + fileName;
            }
            else if (kind == PhotoType.REGION) {
                // 지역 사진
                s3Key = REGION_DIR_NAME + "/" + fileName;

            }
            else {
                // 그 외
                s3Key = ETC_DIR_NAME + "/" + fileName;
            }

            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(file.getSize());
            metadata.setContentType(file.getContentType());

            // s3 에 파일 올리기
            amazonS3.putObject(new PutObjectRequest(bucketName, s3Key, file.getInputStream(), metadata));

            return cloudFrontUrl + "/" + s3Key; // CloudFront URL 반환

        } catch (IOException e) {
            throw new RuntimeException("파일 업로드 실패", e);
        }
    }

    // 파일 삭제
    // cloud front cache 는 24시간 후 자동 소멸 => s3 파일 삭제 후 해당 url 접속할 일 x => 따로 처리 안 함
    public void deleteFile(String imageURL) {

        String s3Key = extractRelativePathFromUrl(imageURL);

        // S3 - 사진 삭제
        System.out.println("🔥 사진 삭제 : " + s3Key);
        amazonS3.deleteObject(bucketName, s3Key);
    }

    // DB 에 저장된 image URL => s3 에 저장된 파일 이름 추출 함수
    private String extractRelativePathFromUrl(String imageUrl) {
        // CloudFront URL 기준 - 경로 추출
        String relativePath = imageUrl.replace(cloudFrontUrl, "");

        // 앞에 '/' 있으면 삭제
        if (relativePath.startsWith("/")) {
            relativePath = relativePath.substring(1); // 첫 번째 '/' 제거
        }

        return relativePath;
    }

    // journal 파일 다운로드
    public ResponseEntity<Resource> downloadFile(String imageURL) throws UnsupportedEncodingException {
        Resource resource = null;

        // key ; 경로 + 파일명
        String key = extractRelativePathFromUrl(imageURL);
        S3Object s3Object = amazonS3.getObject(bucketName, key);
        S3ObjectInputStream s3Is = s3Object.getObjectContent(); // 자동 매핑
        resource = new InputStreamResource(s3Is); // resource 로 매핑

        // 원본 파일 이름 추출
        String fileName = key.substring(key.lastIndexOf("/") + 1); // 마지막 '/' 기준으로 '/' 이후 것을 저장
        fileName = fileName.substring(fileName.lastIndexOf("_") + 1); // 마지막 '_' 기준으로 '_' 이후 것을 저장

        String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8.toString());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM); // 파일 다운로드 형식
        headers.set("Content-Disposition", "inline; filename*=UTF-8''" + encodedFileName);

        return new ResponseEntity<Resource>(resource, headers, HttpStatus.OK);

    }

}
