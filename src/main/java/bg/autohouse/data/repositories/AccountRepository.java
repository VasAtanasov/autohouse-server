package bg.autohouse.data.repositories;

import bg.autohouse.data.models.account.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface AccountRepository
    extends JpaRepository<Account, Long>, JpaSpecificationExecutor<Account> {}