package com.tispace.common.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

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
public class Article extends BaseEntity {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	@Column(name = "title", nullable = false, length = 500)
	private String title;
	
	@Column(name = "description", columnDefinition = "TEXT")
	private String description;
	
	@Column(name = "author", length = 255)
	private String author;
	
	@Column(name = "published_at")
	private LocalDateTime publishedAt;
	
	@Column(name = "category", length = 100)
	private String category;
}


