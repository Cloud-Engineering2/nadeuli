/* S3Config.java
 * nadeuli Service - 여행
 * AWS S3 관련 config
 * 작성자 : 이홍비
 * 최초 작성 일자 : 2025.02.25
 *
 * ========================================================
 * 프로그램 수정 / 보완 이력
 * ========================================================
 * 작업자        날짜        수정 / 보완 내용
 * ========================================================
 * 이홍비    2025.02.25     최초 작성 : S3Config
 * 이홍비    2025.02.26     Cloud Front 추가
 * ========================================================
 */

package nadeuli.config;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.services.s3.AmazonS3;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.cloudfront.AmazonCloudFront;
import com.amazonaws.services.cloudfront.AmazonCloudFrontClientBuilder;

@Configuration
public class S3Config {

    @Value("${cloud.aws.credentials.access-key}")
    private String awsAccessKey;

    @Value("${cloud.aws.credentials.secret-key}")
    private String awsSecretKey;

    @Value("${cloud.aws.region.static}")
    private String region;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    @Value("${cloud.aws.cloudfront.distribution-id}")
    private String cloudFrontDistributionId;

    @Bean
    public AmazonS3 s3client() {
        // S3 사용 인증 객체
        AWSCredentials credentials = new BasicAWSCredentials(awsAccessKey, awsSecretKey);

        // 리전 정보 입력 -> S3 사용 객체 생성
        return AmazonS3ClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(credentials))
                .withRegion(region)
                .build();
    }

    @Bean
    public AmazonCloudFront amazonCloudFront() {
        return AmazonCloudFrontClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(awsAccessKey, awsSecretKey)))
                .withRegion(region)
                .build();
    }
}
