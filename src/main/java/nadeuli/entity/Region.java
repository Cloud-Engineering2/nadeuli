package nadeuli.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "region")
public class Region {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "name", nullable = false, length = 50)
    private String name;


    @ManyToOne
    @JoinColumn(name = "parent_id")
    private Region parent;  // 부모 지역 (시도 -> NULL, 구군 -> 시도 ID)

    @Column(name = "level", nullable = false)
    private int level; // 1: 시·도, 2: 시·군·구

    // 생성자
    public Region(String name, int level, Region parent) {
        this.name = name;
        this.level = level;
        this.parent = parent;
    }

    // 정적 팩토리 메서드
    public static Region of(String name, int level, Region parent) {
        return new Region(name, level, parent);
    }
}
