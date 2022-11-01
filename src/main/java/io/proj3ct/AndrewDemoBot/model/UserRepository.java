/*
 *   Developed by Andrei Muryn© 2022
 */

package io.proj3ct.AndrewDemoBot.model;

import org.springframework.data.repository.CrudRepository;

public interface UserRepository extends CrudRepository<User, Long> {
}
