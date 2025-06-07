import com.work.dto.LoanRequestDTO;
import com.work.model.Customer;
import com.work.model.Loan;
import com.work.repository.CustomerRepository;
import com.work.repository.InstallmentRepository;
import com.work.repository.LoanRepository;
import com.work.service.LoanService;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
@ExtendWith(MockitoExtension.class)
@Disabled
public class LoanServiceTest {


    @InjectMocks
    private LoanService loanService;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private LoanRepository loanRepository;

    @Mock
    private InstallmentRepository installmentRepository;

    @Test
    void shouldRejectWhenInstallmentCountNotAllowed() {
        LoanRequestDTO dto = new LoanRequestDTO();
        dto.setNumberOfInstallment(5);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> loanService.createLoan(dto));
        assertEquals("Taksit sayısı sadece 6, 9, 12, 24 olabilir.", ex.getMessage());
    }

    @Test
    void shouldRejectWhenInterestRateIsOutOfBounds() {
        LoanRequestDTO dto = new LoanRequestDTO();
        dto.setNumberOfInstallment(6);
        dto.setInterestRate(new BigDecimal("0.6"));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> loanService.createLoan(dto));
        assertEquals("Faiz oranı 0.1 ile 0.5 arasında olmalıdır.", ex.getMessage());
    }

    @Test
    void shouldRejectWhenCustomerLimitExceeded() {
        Customer customer = new Customer();
        customer.setCreditLimit(new BigDecimal("10000.00"));
        customer.setUsedCreditLimit(new BigDecimal("9000.00"));

        LoanRequestDTO dto = new LoanRequestDTO();
        dto.setCustomerId(1L);
        dto.setLoanAmount(new BigDecimal("2000.00"));
        dto.setInterestRate(new BigDecimal("0.2"));
        dto.setNumberOfInstallment(6);

        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> loanService.createLoan(dto));
        assertEquals("Müşteri limiti yetersiz.", ex.getMessage());
    }

    @Test
    void shouldCreateLoanSuccessfully() {
        Customer customer = new Customer();
        customer.setId(1L);
        customer.setCreditLimit(new BigDecimal("10000.00"));
        customer.setUsedCreditLimit(BigDecimal.ZERO);

        LoanRequestDTO dto = new LoanRequestDTO();
        dto.setCustomerId(1L);
        dto.setLoanAmount(new BigDecimal("2000.00"));
        dto.setInterestRate(new BigDecimal("0.2"));
        dto.setNumberOfInstallment(6);

        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(loanRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(installmentRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        String result = loanService.createLoan(dto);
        assertEquals("Kredi başarıyla oluşturuldu.", result);

        verify(loanRepository, times(1)).save(any(Loan.class));
        verify(installmentRepository, times(1)).saveAll(anyList());
        verify(customerRepository, times(1)).save(any(Customer.class));
    }
}