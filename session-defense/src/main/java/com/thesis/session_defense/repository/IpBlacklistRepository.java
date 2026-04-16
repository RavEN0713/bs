package com.thesis.session_defense.repository;

import com.thesis.session_defense.entity.IpBlacklist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface IpBlacklistRepository extends JpaRepository<IpBlacklist, Long> {
    boolean existsByIp(String ip);

    @Modifying
    @Transactional
    @Query("delete from IpBlacklist i where i.ip = :ip")
    int deleteByIp(String ip);

    List<IpBlacklist> findAllByOrderByCreateTimeDesc();
}
