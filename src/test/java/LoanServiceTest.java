import com.work.dto.LoanRequestDTO;
import com.work.model.Customer;
import com.work.model.Loan;
import com.work.repository.CustomerRepository;
import com.work.repository.InstallmentRepository;
import com.work.repository.LoanRepository;
import com.work.service.LoanService;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

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

        String result = loanService.createLoan(dto);
        assertEquals("Taksit sayısı sadece 6, 9, 12, 24 olabilir.", result);
    }

    @Test
    void shouldRejectWhenInterestRateIsOutOfBounds() {
        LoanRequestDTO dto = new LoanRequestDTO();
        dto.setNumberOfInstallment(6);
        dto.setInterestRate(0.6);

        String result = loanService.createLoan(dto);
        assertEquals("Faiz oranı 0.1 ile 0.5 arasında olmalıdır.", result);
    }

    @Test
    void shouldRejectWhenCustomerLimitExceeded() {
        Customer customer = new Customer();
        customer.setCreditLimit(10000.0);
        customer.setUsedCreditLimit(9000.0);

        LoanRequestDTO dto = new LoanRequestDTO();
        dto.setCustomerId(1L);
        dto.setLoanAmount(2000.0);
        dto.setInterestRate(0.2);
        dto.setNumberOfInstallment(6);

        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));

        String result = loanService.createLoan(dto);
        assertEquals("Müşteri limiti yetersiz.", result);
    }

    @Test
    void shouldCreateLoanSuccessfully() {
        Customer customer = new Customer();
        customer.setId(1L);
        customer.setCreditLimit(10000.0);
        customer.setUsedCreditLimit(0.0);

        LoanRequestDTO dto = new LoanRequestDTO();
        dto.setCustomerId(1L);
        dto.setLoanAmount(2000.0);
        dto.setInterestRate(0.2);
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