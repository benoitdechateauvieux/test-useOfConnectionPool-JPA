import com.mchange.v2.c3p0.PoolBackedDataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.management.*;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import java.lang.management.ManagementFactory;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 * exo@exoplatform.com
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
    public void createEM_doesnotCreateConnection() throws Exception {
        //Given
        //When
        EntityManager em = emf.createEntityManager();
        //Then
        assertThat(getNumBusyConnection(), is(0));
        em.close();
    }

    @Test
    public void createOpenTransaction_doesnotCreateConnection() throws Exception {
        //Given
        EntityManager em = emf.createEntityManager();
        //When
        em.getTransaction().begin();
        //Then
        assertThat(getNumBusyConnection(), is(0));
        em.close();
    }

    private int getNumBusyConnection() throws Exception {
        MBeanServer mbs    = ManagementFactory.getPlatformMBeanServer();
        ObjectName name    = ObjectName.getInstance("com.mchange.v2.c3p0:type=C3P0Registry");
        AttributeList list = mbs.getAttributes(name, new String[]{"AllPooledDataSources"});
        PoolBackedDataSource dataSource = (PoolBackedDataSource) ((java.util.Set)((Attribute)list.get(0)).getValue()).iterator().next();
        String identityToken = dataSource.getIdentityToken();
        ObjectName nameDs  = ObjectName.getInstance("com.mchange.v2.c3p0:type=PooledDataSource[" + identityToken + "]");
        AttributeList numBusylist = mbs.getAttributes(nameDs, new String[]{"numBusyConnectionsAllUsers"});
        return (Integer)((Attribute)numBusylist.get(0)).getValue();
    }
}
