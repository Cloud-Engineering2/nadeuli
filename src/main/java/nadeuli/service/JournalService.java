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
 * ========================================================
 */

package nadeuli.service;


import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
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

    private final String JOURNAL = "journal";

    // 기행문 조회
    public JournalDTO getJournal(Long ieid) throws NoSuchElementException {
        System.out.println("🔥 기행문 조회 로직 실행됨!");

        Journal journal = journalRepository.findById(ieid)
                .orElseThrow(() -> new NoSuchElementException("해당 방문지의 기행문을 찾을 수 없습니다."));

        return JournalDTO.from(journal);
    }

    // 사진 등록
    public JournalDTO uploadPhoto(Long ieid, MultipartFile file) {
        System.out.println("🔥 기행문 - 사진 올리기 로직 실행됨!");

        ItineraryEvent event = itineraryEventRepository.findById(ieid)
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않는 ieid 입니다."));

        String imageUrl = s3Service.uploadFile(file, JOURNAL);

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

    // 사진 변경
    public JournalDTO modifiedPhoto(Long ieid, MultipartFile file) throws NoSuchElementException {
        System.out.println("🔥 기행문 - 사진 변경 로직 실행됨!");

        Journal journal = journalRepository.findById(ieid)
                .orElseThrow(() -> new NoSuchElementException("해당 방문지의 기행문을 찾을 수 없습니다."));

        // 기존 사진 삭제
        s3Service.deleteFile(journal.getImageUrl(), JOURNAL);

        // 새로운 사진 올리고 image url 저장
        String imageUrl = s3Service.uploadFile(file, JOURNAL);
        journal.saveImageURL(imageUrl);
        journalRepository.save(journal);

        return JournalDTO.from(journal);
    }

    // 사진 삭제
    public JournalDTO deletePhoto(Long ieid) throws NoSuchElementException {
        System.out.println("🔥 기행문 - 사진 삭제 로직 실행됨!");

        Journal journal = journalRepository.findById(ieid)
                .orElseThrow(() -> new NoSuchElementException("해당 방문지의 기행문을 찾을 수 없습니다."));

        // 사진 삭제 후 url 값 null 로 저장
        s3Service.deleteFile(journal.getImageUrl(), JOURNAL);
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

        ItineraryEvent event = itineraryEventRepository.findById(ieid)
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않는 ieid 입니다."));

        Journal journal;
        if (journalRepository.findById(ieid).isPresent()) {
            // 해당 방문지 - 기행문 이미 존재함
            journal = journalRepository.findById(ieid).get();
            journal.saveContent(content); // 글 저장
            journalRepository.save(journal); // 저장
        }
        else {
            // 해당 방문지 - 기행문 존재 x
            journal = Journal.of(event, content, null); // Journal 객체 생성
            journalRepository.save(journal); // 저장
        }

        return JournalDTO.from(journal);
    }

    // 본문 수정
    public JournalDTO modifiedContent(Long ieid, String content) throws NoSuchElementException {
        System.out.println("🔥 기행문 - 본문 수정 로직 실행됨!");

        Journal journal = journalRepository.findById(ieid)
                .orElseThrow(() -> new NoSuchElementException("해당 방문지의 기행문을 찾을 수 없습니다."));

        // 본문 내용 변경 후 저장
        journal.saveContent(content);
        journalRepository.save(journal);

        return JournalDTO.from(journal);

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
