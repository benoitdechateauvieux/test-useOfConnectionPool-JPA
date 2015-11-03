import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.lang.management.ManagementFactory;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.Query;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.mchange.v2.c3p0.PoolBackedDataSource;

/**
 * Created by The eXo Platform SAS Author : eXoPlatform exo@exoplatform.com
 * 9/24/15
 */
public class EntityManagerTest {

  private EntityManagerFactory emf;

  @Before
  public void initEmf() {
    emf = Persistence.createEntityManagerFactory("exo-pu");
  }

  @After
  public void closeEmf() {
    emf.close();
  }

  @Test
  public void createEM_doesntOpenConnection() throws Exception {
    // Given
    // When
    EntityManager em = emf.createEntityManager();
    // Then
    Thread.sleep(2*1000L);
    assertThat(getNumBusyConnection(), is(0));
    em.close();
  }

  @Test
  public void select_doesntImmediatelyReleaseConnection() throws Exception {
    // Given
    EntityManager em = emf.createEntityManager();
    // When
    Query query = em.createNativeQuery("SELECT 1 FROM INFORMATION_SCHEMA.SYSTEM_USERS");
    Integer one = (Integer) query.getSingleResult();
    assertThat(one, is(1));
    // Then
    Thread.sleep(2 * 1000L);
    assertThat(getNumBusyConnection(), is(1));
    em.close();
  }

  @Test
  public void clear_doesntImmediatelyReleaseConnection() throws Exception {
    // Given
    EntityManager em = emf.createEntityManager();
    Query query = em.createNativeQuery("SELECT 1 FROM INFORMATION_SCHEMA.SYSTEM_USERS");
    Integer one = (Integer) query.getSingleResult();
    assertThat(one, is(1));
    // When
    em.clear();
    // Then
    Thread.sleep(2 * 1000L);
    assertThat(getNumBusyConnection(), is(0));
    em.close();
  }

  @Test
  public void beginTransaction_openConnection() throws Exception {
    // Given
    EntityManager em = emf.createEntityManager();
    // When
    em.getTransaction().begin();
    // Then
    Thread.sleep(2*1000L);
    assertThat(getNumBusyConnection(), is(1));
    em.close();
  }

  @Test
  public void commitTransaction_doesntCloseConnection() throws Exception {
    // Given
    EntityManager em = emf.createEntityManager();
    em.getTransaction().begin();
    assertThat(getNumBusyConnection(), is(1));
    // When
    em.getTransaction().commit();
    // Then
    Thread.sleep(2*1000L);
    assertThat(getNumBusyConnection(), is(1));
    em.close();
  }

  @Test
  public void rollbackTransaction_doesntCloseConnection() throws Exception {
    // Given
    EntityManager em = emf.createEntityManager();
    em.getTransaction().begin();
    assertThat(getNumBusyConnection(), is(1));
    // When
    em.getTransaction().rollback();
    // Then
    Thread.sleep(2*1000L);
    assertThat(getNumBusyConnection(), is(1));
    em.close();
  }

  @Test
  public void closeEM_afterCloseTransaction_closeConnection() throws Exception {
    // Given
    EntityManager em = emf.createEntityManager();
    em.getTransaction().begin();
    assertThat(getNumBusyConnection(), is(1));
    em.getTransaction().commit();
    assertThat(getNumBusyConnection(), is(1));
    // When
    em.close();
    // Then
    Thread.sleep(2 * 1000L);
    assertThat(getNumBusyConnection(), is(0));
  }

  @Test
  public void closeEmDuringTransaction_doesntImmediatelyReleaseConnection() throws Exception {
    // Given
    EntityManager em = emf.createEntityManager();
    em.getTransaction().begin();
    // When
    em.close();
    // Then
    assertFalse(em.isOpen());
    assertTrue(em.getTransaction().isActive());
    assertThat(getNumBusyConnection(), is(1));
    em.getTransaction().commit();
    assertThat(getNumBusyConnection(), is(0));
  }

  private int getNumBusyConnection() throws Exception {
    MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
    ObjectName name = ObjectName.getInstance("com.mchange.v2.c3p0:type=C3P0Registry");
    AttributeList list = mbs.getAttributes(name, new String[] { "AllPooledDataSources" });
    PoolBackedDataSource dataSource = (PoolBackedDataSource) ((java.util.Set) ((Attribute) list.get(0)).getValue()).iterator()
                                                                                                                   .next();
    String identityToken = dataSource.getIdentityToken();
    ObjectName nameDs = ObjectName.getInstance("com.mchange.v2.c3p0:type=PooledDataSource[" + identityToken + "]");
    AttributeList numBusylist = mbs.getAttributes(nameDs, new String[] { "numBusyConnectionsAllUsers" });
    return (Integer) ((Attribute) numBusylist.get(0)).getValue();
  }
}
