package com.chicu.trader.bot.repository;

import com.chicu.trader.bot.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}
