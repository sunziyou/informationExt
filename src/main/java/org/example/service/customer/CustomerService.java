package org.example.service.customer;

import java.util.List;

public interface CustomerService {
       List<String> queryByName(String customer);
       List<String> queryByVagueName(String customer);
       List<Projectbean> queryProject(String customer);
       List<CustomBean> queryCustomer(String customer);
}
