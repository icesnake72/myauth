CREATE TABLE `refresh_tokens` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `expires_at` datetime(6) NOT NULL,
  `is_revoked` bit(1) NOT NULL,
  `last_used_at` datetime(6) DEFAULT NULL,
  `token` varchar(500) NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FK1lih5y2npsf8u5o3vhdb9y0os` (`user_id`),
  CONSTRAINT `FK1lih5y2npsf8u5o3vhdb9y0os` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=29 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci



// oauth 2.0 추가
ALTER TABLE users
ADD COLUMN provider VARCHAR(20) DEFAULT 'LOCAL' COMMENT 'LOCAL, KAKAO, GOOGLE 등',
ADD COLUMN provider_id VARCHAR(100) NULL COMMENT '카카오 회원번호 등 OAuth 제공자의 고유 ID',
ADD COLUMN profile_image VARCHAR(500) NULL COMMENT '프로필 이미지 URL';

-- 2. 이메일을 nullable로 변경 (카카오 ID로만 가입하는 경우 대비)
ALTER TABLE users
MODIFY COLUMN email VARCHAR(100) NULL;

-- 3. 유니크 제약조건 추가 (provider + provider_id 조합은 유니크)
ALTER TABLE users
ADD UNIQUE KEY uk_provider_provider_id (provider, provider_id);




