package com.levelup.backend.config;

import com.levelup.backend.product.Product;
import com.levelup.backend.product.ProductRepository;
import com.levelup.backend.user.Role;
import com.levelup.backend.user.RoleName;
import com.levelup.backend.user.RoleRepository;
import com.levelup.backend.user.User;
import com.levelup.backend.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {
    
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final PasswordEncoder passwordEncoder;
    
    @Override
    public void run(String... args) {
        initRoles();
        initUsers();
        initProducts();
        
        log.info("✅ Datos iniciales cargados correctamente");
    }
    
    private void initRoles() {
        if (roleRepository.count() == 0) {
            Arrays.stream(RoleName.values()).forEach(roleName -> 
                roleRepository.save(Role.builder().name(roleName).build())
            );
            log.info("📌 Roles creados: ADMIN, VENDEDOR, CLIENTE");
        }
    }
    
    private void initUsers() {
        if (userRepository.count() == 0) {
            Role adminRole = roleRepository.findByName(RoleName.ADMIN).orElseThrow();
            Role vendedorRole = roleRepository.findByName(RoleName.VENDEDOR).orElseThrow();
            Role clienteRole = roleRepository.findByName(RoleName.CLIENTE).orElseThrow();
            
            // Usuario Admin
            User admin = User.builder()
                    .name("Administrador")
                    .email("admin@levelup.cl")
                    .password(passwordEncoder.encode("admin123"))
                    .enabled(true)
                    .roles(new HashSet<>(Arrays.asList(adminRole, vendedorRole, clienteRole)))
                    .build();
            userRepository.save(admin);
            
            // Usuario Vendedor
            User vendedor = User.builder()
                    .name("Vendedor Demo")
                    .email("vendedor@levelup.cl")
                    .password(passwordEncoder.encode("vendedor123"))
                    .enabled(true)
                    .roles(new HashSet<>(Arrays.asList(vendedorRole, clienteRole)))
                    .build();
            userRepository.save(vendedor);
            
            // Usuario Cliente
            User cliente = User.builder()
                    .name("Cliente Demo")
                    .email("cliente@levelup.cl")
                    .password(passwordEncoder.encode("cliente123"))
                    .enabled(true)
                    .roles(Collections.singleton(clienteRole))
                    .build();
            userRepository.save(cliente);
            
            log.info("👤 Usuarios creados: admin@levelup.cl, vendedor@levelup.cl, cliente@levelup.cl");
        }
    }
    
    private void initProducts() {
        if (productRepository.count() == 0) {
            List<Product> products = Arrays.asList(
                // Consolas
                Product.builder()
                        .name("PlayStation 5 Standard Edition")
                        .category("Consolas")
                        .brand("Sony")
                        .price(649990)
                        .originalPrice(699990)
                        .discount(7)
                        .stock(15)
                        .description("Consola PlayStation 5 con lector de discos. Incluye control DualSense.")
                        .image("https://images.unsplash.com/photo-1606813907291-d86efa9b94db?w=500")
                        .featured(true)
                        .isOffer(true)
                        .rating(4.8)
                        .reviews(245)
                        .build(),
                
                Product.builder()
                        .name("Xbox Series X")
                        .category("Consolas")
                        .brand("Microsoft")
                        .price(599990)
                        .originalPrice(649990)
                        .discount(8)
                        .stock(12)
                        .description("La consola Xbox más potente de la historia. 12 teraflops de poder gráfico.")
                        .image("https://images.unsplash.com/photo-1621259182978-fbf93132d53d?w=500")
                        .featured(true)
                        .isOffer(true)
                        .rating(4.7)
                        .reviews(189)
                        .build(),
                
                Product.builder()
                        .name("Nintendo Switch OLED")
                        .category("Consolas")
                        .brand("Nintendo")
                        .price(399990)
                        .originalPrice(419990)
                        .discount(5)
                        .stock(20)
                        .description("Nintendo Switch con pantalla OLED de 7 pulgadas y soporte ajustable.")
                        .image("https://images.unsplash.com/photo-1578303512597-81e6cc155b3e?w=500")
                        .featured(true)
                        .isOffer(false)
                        .rating(4.9)
                        .reviews(312)
                        .build(),
                
                // Videojuegos
                Product.builder()
                        .name("The Legend of Zelda: Tears of the Kingdom")
                        .category("Videojuegos")
                        .brand("Nintendo")
                        .price(54990)
                        .originalPrice(64990)
                        .discount(15)
                        .stock(50)
                        .description("Secuela de Breath of the Wild. Explora Hyrule como nunca antes.")
                        .image("https://images.unsplash.com/photo-1550745165-9bc0b252726f?w=500")
                        .featured(true)
                        .isOffer(true)
                        .rating(4.9)
                        .reviews(1024)
                        .build(),
                
                Product.builder()
                        .name("God of War Ragnarök")
                        .category("Videojuegos")
                        .brand("Sony")
                        .price(49990)
                        .originalPrice(59990)
                        .discount(17)
                        .stock(35)
                        .description("Kratos y Atreus enfrentan el Ragnarök nórdico.")
                        .image("https://images.unsplash.com/photo-1592155931584-901ac15763e3?w=500")
                        .featured(true)
                        .isOffer(true)
                        .rating(4.8)
                        .reviews(876)
                        .build(),
                
                Product.builder()
                        .name("FIFA 24")
                        .category("Videojuegos")
                        .brand("EA Sports")
                        .price(44990)
                        .originalPrice(49990)
                        .discount(10)
                        .stock(100)
                        .description("La última entrega de la saga FIFA con HyperMotion V.")
                        .image("https://images.unsplash.com/photo-1493711662062-fa541f7f76fc?w=500")
                        .featured(false)
                        .isOffer(true)
                        .rating(4.2)
                        .reviews(543)
                        .build(),
                
                Product.builder()
                        .name("Hogwarts Legacy")
                        .category("Videojuegos")
                        .brand("Warner Bros")
                        .price(47990)
                        .originalPrice(54990)
                        .discount(13)
                        .stock(40)
                        .description("Vive tu aventura en el mundo mágico de Harry Potter.")
                        .image("https://images.unsplash.com/photo-1618336753974-aae8e04506aa?w=500")
                        .featured(true)
                        .isOffer(true)
                        .rating(4.6)
                        .reviews(654)
                        .build(),
                
                // Accesorios
                Product.builder()
                        .name("Control DualSense PS5")
                        .category("Accesorios")
                        .brand("Sony")
                        .price(69990)
                        .originalPrice(79990)
                        .discount(13)
                        .stock(30)
                        .description("Control inalámbrico DualSense con gatillos adaptativos y feedback háptico.")
                        .image("https://images.unsplash.com/photo-1592840496694-26d035b52b48?w=500")
                        .featured(false)
                        .isOffer(true)
                        .rating(4.7)
                        .reviews(234)
                        .build(),
                
                Product.builder()
                        .name("Audífonos HyperX Cloud II")
                        .category("Accesorios")
                        .brand("HyperX")
                        .price(79990)
                        .originalPrice(89990)
                        .discount(11)
                        .stock(25)
                        .description("Audífonos gaming con sonido envolvente 7.1 y micrófono desmontable.")
                        .image("https://images.unsplash.com/photo-1599669454699-248893623440?w=500")
                        .featured(false)
                        .isOffer(true)
                        .rating(4.6)
                        .reviews(189)
                        .build(),
                
                Product.builder()
                        .name("Teclado Mecánico Logitech G Pro")
                        .category("Accesorios")
                        .brand("Logitech")
                        .price(129990)
                        .originalPrice(149990)
                        .discount(13)
                        .stock(18)
                        .description("Teclado mecánico compacto con switches GX Blue.")
                        .image("https://images.unsplash.com/photo-1541140532154-b024d705b90a?w=500")
                        .featured(false)
                        .isOffer(true)
                        .rating(4.5)
                        .reviews(156)
                        .build(),
                
                Product.builder()
                        .name("Mouse Razer DeathAdder V3")
                        .category("Accesorios")
                        .brand("Razer")
                        .price(89990)
                        .originalPrice(99990)
                        .discount(10)
                        .stock(22)
                        .description("Mouse ergonómico para gaming con sensor Focus Pro 30K.")
                        .image("https://images.unsplash.com/photo-1527864550417-7fd91fc51a46?w=500")
                        .featured(false)
                        .isOffer(true)
                        .rating(4.7)
                        .reviews(198)
                        .build(),
                
                // Merchandise
                Product.builder()
                        .name("Polera Zelda Tears of the Kingdom")
                        .category("Merchandise")
                        .brand("Nintendo")
                        .price(19990)
                        .originalPrice(24990)
                        .discount(20)
                        .stock(50)
                        .description("Polera oficial de The Legend of Zelda: Tears of the Kingdom.")
                        .image("https://images.unsplash.com/photo-1576566588028-4147f3842f27?w=500")
                        .featured(false)
                        .isOffer(true)
                        .rating(4.4)
                        .reviews(87)
                        .build(),
                
                Product.builder()
                        .name("Figura Amiibo Link")
                        .category("Merchandise")
                        .brand("Nintendo")
                        .price(24990)
                        .originalPrice(29990)
                        .discount(17)
                        .stock(15)
                        .description("Figura Amiibo de Link compatible con juegos de Nintendo Switch.")
                        .image("https://images.unsplash.com/photo-1558618666-fcd25c85cd64?w=500")
                        .featured(false)
                        .isOffer(true)
                        .rating(4.8)
                        .reviews(112)
                        .build(),
                
                Product.builder()
                        .name("Taza PlayStation Symbols")
                        .category("Merchandise")
                        .brand("Sony")
                        .price(9990)
                        .originalPrice(12990)
                        .discount(23)
                        .stock(100)
                        .description("Taza de cerámica con los símbolos icónicos de PlayStation.")
                        .image("https://images.unsplash.com/photo-1514228742587-6b1558fcca3d?w=500")
                        .featured(false)
                        .isOffer(true)
                        .rating(4.3)
                        .reviews(65)
                        .build()
            );
            
            productRepository.saveAll(products);
            log.info("🎮 {} productos cargados", products.size());
        }
    }
}
