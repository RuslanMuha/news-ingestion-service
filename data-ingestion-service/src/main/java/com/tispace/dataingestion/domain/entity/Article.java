package com.tispace.dataingestion.domain.entity;

import com.github.f4b6a3.uuid.UuidCreator;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "articles", indexes = {
	@Index(name = "idx_category", columnList = "category"),
	@Index(name = "idx_published_at", columnList = "published_at")
}, uniqueConstraints = {
	@UniqueConstraint(name = "uk_articles_title", columnNames = "title")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
public class Article extends BaseEntity {
	
	@Id
	@Column(name = "id", columnDefinition = "UUID", nullable = false, updatable = false)
	private UUID id;
	
	@PrePersist
	protected void onCreate() {
		if (id == null) {
			id = UuidCreator.getTimeOrderedEpoch();
		}
	}

    @EqualsAndHashCode.Include
	@Column(name = "title", nullable = false, columnDefinition = "TEXT")
	private String title;
	
	@Column(name = "description", columnDefinition = "TEXT")
	private String description;

    @EqualsAndHashCode.Include
	@Column(name = "author", columnDefinition = "TEXT")
	private String author;
	
	@Column(name = "published_at")
	private LocalDateTime publishedAt;
	
	@Column(name = "category", columnDefinition = "TEXT")
	private String category;
}

