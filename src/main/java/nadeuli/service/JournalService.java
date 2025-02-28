/* JournalService.java
 * Journal 서비스
 * 작성자 : 박한철
 * 최초 작성 날짜 : 2025-02-25
 *
 * ========================================================
 * 프로그램 수정 / 보완 이력
 * ========================================================
 * 작업자        날짜        수정 / 보완 내용
 * ========================================================
 * 이홍비    2025.02.25     기행 crud
 * 이홍비    2025.02.26     추후 기능 확장 (프로필 사진) 고려 => JOURNAL 구분 처리
 * 이홍비    2025.02.28     S3Service.deleteFile() 변경에 따른 수정
 *                         getJournal() 에 따른 uploadPhoto(), writeContent() 정리 => 간략화
 *                         modifiedContent() 와 modifiedPhoto() 실행 방식 결정
 * ========================================================
 */

package nadeuli.service;


import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import nadeuli.common.PhotoType;
import nadeuli.dto.JournalDTO;
import nadeuli.entity.ItineraryEvent;
import nadeuli.entity.Journal;
import nadeuli.repository.ItineraryEventRepository;
import nadeuli.repository.JournalRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
@Transactional
public class JournalService {
    private final JournalRepository journalRepository;
    private final ItineraryEventRepository itineraryEventRepository;
    private final S3Service s3Service;

    // 특정 기행문 조회 - if 없다 => 생성하도록 구현 : 기행문 작성 page == 기행문 조회 page
    public JournalDTO getJournal(Long ieid) throws NoSuchElementException {
        System.out.println("🔥 기행문 조회 로직 실행됨!");

        Journal journal = journalRepository.findById(ieid)
                .orElseGet(() -> {
                    // 존재하지 않을 경우 새로운 기행문 생성 및 저장
                    Journal newJournal = Journal.of(itineraryEventRepository.findById(ieid).orElseThrow(() -> new NoSuchElementException("해당 방문지를 찾을 수 없습니다.")), null, null);
                    journalRepository.save(newJournal);

                    return newJournal;
                });

        return JournalDTO.from(journal);
    }

    // 사진 등록
    public JournalDTO uploadPhoto(Long ieid, MultipartFile file) {
        System.out.println("🔥 기행문 - 사진 올리기 로직 실행됨!");

        Journal journal = journalRepository.findById(ieid)
                .orElseThrow(() -> new NoSuchElementException("해당 방문지의 기행문을 찾을 수 없습니다."));

        // s3 에 새로운 사진 파일 올리고
        String imageUrl = s3Service.uploadFile(file, PhotoType.JOURNAL);

        journal.saveImageURL(imageUrl); // 사진 url 저장
        journalRepository.save(journal); // 저장

        return JournalDTO.from(journal);
    }

    // 사진 변경 : 성능 고려 (실행 시간 측정 결과 : 80ms) - controller 에서 현재 사용 중
    public JournalDTO modifiedPhotoVer1(Long ieid, MultipartFile file) throws NoSuchElementException {
        System.out.println("🔥 기행문 - 사진 변경 로직 실행됨!");

        Journal journal = journalRepository.findById(ieid)
                .orElseThrow(() -> new NoSuchElementException("해당 방문지의 기행문을 찾을 수 없습니다."));

        // 기존 사진 삭제
        s3Service.deleteFile(journal.getImageUrl());

        // s3 에 새로운 사진 파일 올리고
        String imageUrl = s3Service.uploadFile(file, PhotoType.JOURNAL);

        journal.saveImageURL(imageUrl); // 사진 url 저장
        journalRepository.save(journal); // 저장

        return JournalDTO.from(journal);
    }

    // 사진 변경 : 코드 재사용 => 일관성 + 가독성 + 유지 보수 (overhead o -실행 시간 측정 결과 : 338ms)
    public JournalDTO modifiedPhotoVer2(Long ieid, MultipartFile file) throws NoSuchElementException {
        System.out.println("🔥 기행문 - 사진 변경 로직 실행됨!");

        Journal journal = journalRepository.findById(ieid)
                .orElseThrow(() -> new NoSuchElementException("해당 방문지의 기행문을 찾을 수 없습니다."));

        // 기존 사진 삭제
        s3Service.deleteFile(journal.getImageUrl());

        // uploadPhoto(ieid, file) 로 사진 올림
        // => 코드 일관성 + 가독성 선택
        // overhead 미미
        return uploadPhoto(ieid, file);
    }



    // 사진 삭제
    public JournalDTO deletePhoto(Long ieid) throws NoSuchElementException {
        System.out.println("🔥 기행문 - 사진 삭제 로직 실행됨!");

        Journal journal = journalRepository.findById(ieid)
                .orElseThrow(() -> new NoSuchElementException("해당 방문지의 기행문을 찾을 수 없습니다."));

        // 사진 삭제 후 url 값 null 로 저장
        s3Service.deleteFile(journal.getImageUrl());
        journal.saveImageURL(null);
        journalRepository.save(journal);

        return JournalDTO.from(journal);
    }

    // 사진 등록 - local test
    public JournalDTO uploadPhotoTest(Long ieid, String imageUrl) {
        ItineraryEvent event = itineraryEventRepository.findById(ieid)
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않는 ieid 입니다."));

        Journal journal;
        if (journalRepository.findById(ieid).isPresent()) {
            // 해당 방문지 - 기행문 이미 존재함
            journal = journalRepository.findById(ieid).get();
            journal.saveImageURL(imageUrl); // 사진 url 저장
            journalRepository.save(journal); // 저장
        }
        else {
            // 해당 방문지 - 기행문 존재 x
            journal = Journal.of(event, null, imageUrl); // Journal 객체 생성
            journalRepository.save(journal); // 저장
        }

        return JournalDTO.from(journal);
    }


    // 본문 작성
    public JournalDTO writeContent(Long ieid, String content) {
        System.out.println("🔥 기행문 - 본문 작성 로직 실행됨!");

        Journal journal = journalRepository.findById(ieid)
                .orElseThrow(() -> new NoSuchElementException("해당 방문지의 기행문을 찾을 수 없습니다."));

        journal.saveContent(content); // 글 저장
        journalRepository.save(journal); // 저장

        return JournalDTO.from(journal);
    }

    // 본문 수정 - 직접 호출
    public JournalDTO modifiedContentVer1(Long ieid, String content) throws NoSuchElementException {
        System.out.println("🔥 기행문 - 본문 수정 로직 실행됨!");

        Journal journal = journalRepository.findById(ieid)
                .orElseThrow(() -> new NoSuchElementException("해당 방문지의 기행문을 찾을 수 없습니다."));

        // 본문 내용 변경 후 저장
        journal.saveContent(content);
        journalRepository.save(journal);

        return JournalDTO.from(journal);

    }

    // 본문 수정 - 함수 호출
    public JournalDTO modifiedContentVer2(Long ieid, String content) throws NoSuchElementException {
        System.out.println("🔥 기행문 - 본문 수정 로직 실행됨!");

        return writeContent(ieid, content);
    }

        // 본문 삭제
    public JournalDTO deleteContent(Long ieid) throws NoSuchElementException {
        System.out.println("🔥 기행문 - 본문 삭제 로직 실행됨!");

        Journal journal = journalRepository.findById(ieid)
                .orElseThrow(() -> new NoSuchElementException("해당 방문지의 기행문을 찾을 수 없습니다."));

        // null 값으로 변경 후 저장
        journal.saveContent(null);
        journalRepository.save(journal);

        return JournalDTO.from(journal);
    }
}
